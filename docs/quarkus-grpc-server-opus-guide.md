# Comprehensive Guide to Building gRPC Servers with Quarkus

**Author:** Your Friendly Quarkus Guide  
**Last Updated:** January 2026

Welcome, fellow developer! This comprehensive guide will walk you through creating robust gRPC servers using the Quarkus framework. Whether you're new to gRPC or looking to implement it with Quarkus, this guide covers everything from fundamental concepts to hands-on implementation with a practical MongoDB example.

---

## Table of Contents

1. [Introduction to gRPC](#1-introduction-to-grpc)
2. [Understanding Protocol Buffers (Protobuf)](#2-understanding-protocol-buffers-protobuf)
3. [The Four gRPC Communication Modes](#3-the-four-grpc-communication-modes)
4. [gRPC with Quarkus - Why It's a Perfect Match](#4-grpc-with-quarkus---why-its-a-perfect-match)
5. [Project Setup - Building a Car CRUD Service](#5-project-setup---building-a-car-crud-service)
6. [Implementing the Active Record Pattern with MongoDB](#6-implementing-the-active-record-pattern-with-mongodb)
7. [Defining the Protobuf Service](#7-defining-the-protobuf-service)
8. [Implementing gRPC Service Methods](#8-implementing-grpc-service-methods)
9. [Error Handling and Interceptors](#9-error-handling-and-interceptors)
10. [Testing Your gRPC Server](#10-testing-your-grpc-server)
11. [Configuration Reference](#11-configuration-reference)
12. [Useful Resources for Continued Learning](#12-useful-resources-for-continued-learning)

---

## 1. Introduction to gRPC

### What is gRPC?

**gRPC** (gRPC Remote Procedure Call) is a modern, high-performance framework developed by Google for building distributed systems. It enables client and server applications to communicate transparently, making it easier to create connected services.

### Key Features

| Feature | Description |
|---------|-------------|
| **HTTP/2 Transport** | Built on HTTP/2, providing multiplexing, flow control, header compression, and bidirectional streaming |
| **Protocol Buffers** | Uses Protobuf as its Interface Definition Language (IDL) and message format for compact, efficient serialization |
| **Language Agnostic** | Supports multiple programming languages (Java, Go, Python, C++, etc.) |
| **Bidirectional Streaming** | Enables real-time, bidirectional communication between client and server |
| **Strong Typing** | Enforces type safety through generated code from `.proto` definitions |
| **Deadline/Timeout Support** | Built-in mechanism for handling timeouts and deadlines |

### Why HTTP/2 Matters

gRPC leverages HTTP/2's powerful features:

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP/2 Connection                        │
├─────────────────────────────────────────────────────────────┤
│  Stream 1 ──────────────────────────────────────────►       │
│  Stream 2 ◄──────────────────────────────────────────       │
│  Stream 3 ◄────────────────────────────────────────►        │
│  ... (Multiple streams on single connection)                │
└─────────────────────────────────────────────────────────────┘
```

- **Multiplexing:** Multiple requests/responses over a single TCP connection
- **Binary Framing:** More efficient parsing than HTTP/1.1 text-based protocol
- **Header Compression (HPACK):** Reduces overhead for repeated headers
- **Flow Control:** Prevents overwhelming receivers with data
- **Long-Lived Connections:** Reduces connection establishment overhead

---

## 2. Understanding Protocol Buffers (Protobuf)

### What are Protocol Buffers?

Protocol Buffers (Protobuf) is Google's language-neutral, platform-neutral mechanism for serializing structured data. Think of it as a more efficient, strongly-typed alternative to JSON or XML.

### Key Benefits

| Aspect | JSON | Protocol Buffers |
|--------|------|------------------|
| **Format** | Text (human-readable) | Binary (compact) |
| **Size** | Larger | 3-10x smaller |
| **Speed** | Slower parsing | Faster serialization/deserialization |
| **Schema** | Optional (JSON Schema) | Required (`.proto` files) |
| **Type Safety** | Weak | Strong (code generation) |

### Proto File Structure

A typical `.proto` file contains:

```protobuf
// 1. Syntax version declaration
syntax = "proto3";

// 2. Package and options
option java_multiple_files = true;
option java_package = "com.example.grpc";
option java_outer_classname = "MyProto";

package mypackage;

// 3. Message definitions (data structures)
message MyRequest {
    string name = 1;      // field_name = field_number
    int32 age = 2;
    repeated string tags = 3;  // 'repeated' means array/list
}

message MyResponse {
    string result = 1;
    bool success = 2;
}

// 4. Service definitions (API endpoints)
service MyService {
    rpc MyMethod (MyRequest) returns (MyResponse);
}
```

### Field Types Reference

| Proto Type | Java Type | Description |
|------------|-----------|-------------|
| `string` | `String` | UTF-8 encoded text |
| `int32` / `int64` | `int` / `long` | Signed integers |
| `uint32` / `uint64` | `int` / `long` | Unsigned integers |
| `float` / `double` | `float` / `double` | Floating-point numbers |
| `bool` | `boolean` | Boolean value |
| `bytes` | `ByteString` | Raw bytes |
| `repeated T` | `List<T>` | Array/List of type T |

---

## 3. The Four gRPC Communication Modes

gRPC supports four distinct communication patterns, each suited for different use cases:

### 3.1 Unary RPC (Request-Response)

```
┌────────┐                        ┌────────┐
│ Client │ ────── Request ──────► │ Server │
│        │ ◄───── Response ────── │        │
└────────┘                        └────────┘
```

**Definition:**
```protobuf
rpc GetById (GetRequest) returns (GetResponse);
```

**Use Cases:**
- Fetching a single record by ID
- Creating a new resource
- Simple query operations
- Authentication requests

**Characteristics:**
- Simplest pattern, similar to HTTP REST
- Client sends one message, receives one response
- Suitable for most traditional request-response scenarios

---

### 3.2 Server Streaming RPC

```
┌────────┐                        ┌────────┐
│ Client │ ────── Request ──────► │ Server │
│        │ ◄───── Response 1 ──── │        │
│        │ ◄───── Response 2 ──── │        │
│        │ ◄───── Response 3 ──── │        │
│        │ ◄───── ... ─────────── │        │
│        │ ◄───── Complete ─────  │        │
└────────┘                        └────────┘
```

**Definition:**
```protobuf
rpc ListAll (ListRequest) returns (stream ItemResponse);
```

**Use Cases:**
- Listing large datasets (pagination alternative)
- Downloading files in chunks
- Real-time data feeds (stock prices, news)
- Log streaming

**Characteristics:**
- Client sends one request
- Server returns multiple responses as a stream
- Message ordering is guaranteed
- Client reads until stream ends

---

### 3.3 Client Streaming RPC

```
┌────────┐                        ┌────────┐
│ Client │ ────── Request 1 ────► │ Server │
│        │ ────── Request 2 ────► │        │
│        │ ────── Request 3 ────► │        │
│        │ ────── ... ──────────► │        │
│        │ ────── Complete ─────► │        │
│        │ ◄───── Response ────── │        │
└────────┘                        └────────┘
```

**Definition:**
```protobuf
rpc BulkUpload (stream UploadRequest) returns (UploadSummary);
```

**Use Cases:**
- Uploading files in chunks
- Bulk data import operations
- Aggregating client-side data
- Sensor data collection

**Characteristics:**
- Client sends multiple messages as a stream
- Server responds with a single summary/result
- Efficient for batch operations
- Reduces network round trips

---

### 3.4 Bidirectional Streaming RPC

```
┌────────┐                        ┌────────┐
│ Client │ ────── Request 1 ────► │ Server │
│        │ ◄───── Response 1 ──── │        │
│        │ ────── Request 2 ────► │        │
│        │ ────── Request 3 ────► │        │
│        │ ◄───── Response 2 ──── │        │
│        │ ◄───── Response 3 ──── │        │
│        │       ... (continues)  │        │
└────────┘                        └────────┘
```

**Definition:**
```protobuf
rpc Chat (stream ChatMessage) returns (stream ChatMessage);
```

**Use Cases:**
- Real-time chat applications
- Multiplayer gaming
- Live collaboration tools
- Continuous data synchronization
- Interactive validation/processing

**Characteristics:**
- Both streams are independent
- Can be synchronized or asynchronous
- Most flexible but complex pattern
- Ideal for real-time applications

---

### Communication Mode Summary

| Mode | Client Messages | Server Messages | Best For |
|------|-----------------|-----------------|----------|
| **Unary** | 1 | 1 | Simple CRUD operations |
| **Server Streaming** | 1 | Many | Large data retrieval, real-time feeds |
| **Client Streaming** | Many | 1 | Bulk uploads, data aggregation |
| **Bidirectional** | Many | Many | Real-time, interactive applications |

---

## 4. gRPC with Quarkus - Why It's a Perfect Match

### Quarkus gRPC Integration

Quarkus provides first-class support for gRPC through the `quarkus-grpc` extension, offering:

- **Reactive-First Design:** Uses Mutiny (Quarkus's reactive library) for non-blocking I/O
- **Dev Mode:** Live reload with automatic proto file compilation
- **Native Compilation:** Full GraalVM native image support
- **CDI Integration:** Dependency injection for gRPC services
- **Health Checks:** Built-in health check support

### Mutiny Types for gRPC

Quarkus uses Mutiny's reactive types for gRPC:

| gRPC Mode | Return Type | Description |
|-----------|-------------|-------------|
| Unary | `Uni<Response>` | Emits 0 or 1 item, then completes |
| Server Streaming | `Multi<Response>` | Emits 0 to N items, then completes |
| Client Streaming | `Uni<Response>` with `Multi<Request>` param | Consumes stream, returns single result |
| Bidirectional | `Multi<Response>` with `Multi<Request>` param | Both input and output are streams |

---

## 5. Project Setup - Building a Car CRUD Service

Let's build a complete gRPC server that manages `Car` entities using MongoDB and the Active Record pattern.

### Prerequisites

Before we begin, ensure you have:

- ☑️ **JDK 17+** installed
- ☑️ **Apache Maven 3.8+** or Gradle
- ☑️ **Docker** (for running MongoDB locally)
- ☑️ **Quarkus CLI** (optional, but recommended)
- ☑️ **grpcurl** or similar tool (for testing)

### Step 1: Create the Quarkus Project

**Using Quarkus CLI (Recommended):**

```bash
quarkus create app org.acme:car-grpc-service \
    --extension="grpc,mongodb-panache" \
    --no-code
    
cd car-grpc-service
```

**Using Maven directly:**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.0:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=car-grpc-service \
    -Dextensions="grpc,mongodb-panache" \
    -DnoCode
    
cd car-grpc-service
```

### Step 2: Verify Project Structure

After creation, your project structure should look like:

```
car-grpc-service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   ├── proto/          # Proto files go here
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
└── README.md
```

### Step 3: Verify Dependencies

Your `pom.xml` should include these dependencies:

```xml
<dependencies>
    <!-- gRPC Extension -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-grpc</artifactId>
    </dependency>
    
    <!-- MongoDB with Panache (Reactive) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-mongodb-panache</artifactId>
    </dependency>
    
    <!-- Arc (CDI) - Usually included by default -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-arc</artifactId>
    </dependency>
    
    <!-- Test dependencies -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-junit5</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 6. Implementing the Active Record Pattern with MongoDB

### What is the Active Record Pattern?

The **Active Record** pattern combines data access and domain logic in a single class. Each entity knows how to persist and retrieve itself from the database, eliminating the need for separate repository classes.

**Key Characteristics:**
- Entity extends a base class that provides CRUD operations
- Static methods for database queries (e.g., `Car.findById()`)
- Instance methods for persisting/updating (e.g., `car.persist()`)
- No separate Repository or DAO classes needed

### Step 4: Create the Car Entity

Create the file `src/main/java/org/acme/entity/Car.java`:

```java
package org.acme.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;

/**
 * Car entity using the Active Record pattern.
 * 
 * This class combines the entity definition with database operations.
 * By extending ReactivePanacheMongoEntity, we get:
 * - Auto-generated ObjectId field
 * - Reactive CRUD operations (persist, update, delete)
 * - Static finder methods (findById, listAll, find, etc.)
 */
@MongoEntity(collection = "cars")
public class Car extends ReactivePanacheMongoEntity {

    // ========== Entity Fields ==========
    public String brand;
    public String model;
    public int year;
    public String color;
    public double price;

    // ========== Constructors ==========
    
    public Car() {
        // Required for Panache
    }

    public Car(String brand, String model, int year, String color, double price) {
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
    }

    // ========== Custom Query Methods (Active Record Style) ==========

    /**
     * Find all cars by brand name (case-insensitive).
     */
    public static Multi<Car> findByBrand(String brand) {
        return find("brand", brand).stream();
    }

    /**
     * Find all cars manufactured after a specific year.
     */
    public static Multi<Car> findNewerThan(int year) {
        return find("year > ?1", year).stream();
    }

    /**
     * Find all cars within a price range.
     */
    public static Multi<Car> findByPriceRange(double minPrice, double maxPrice) {
        return find("price >= ?1 and price <= ?2", minPrice, maxPrice).stream();
    }

    /**
     * Count cars by brand.
     */
    public static Uni<Long> countByBrand(String brand) {
        return count("brand", brand);
    }

    /**
     * Delete all cars by brand.
     */
    public static Uni<Long> deleteByBrand(String brand) {
        return delete("brand", brand);
    }

    /**
     * Check if a car with specific brand and model exists.
     */
    public static Uni<Boolean> existsByBrandAndModel(String brand, String model) {
        return count("brand = ?1 and model = ?2", brand, model)
                .map(count -> count > 0);
    }

    // ========== Helper Methods ==========

    /**
     * Validates car data before persistence.
     */
    public boolean isValid() {
        return brand != null && !brand.isBlank()
                && model != null && !model.isBlank()
                && year >= 1886  // First car was invented in 1886
                && year <= 2030
                && price >= 0;
    }

    @Override
    public String toString() {
        return String.format("Car{id=%s, brand='%s', model='%s', year=%d, color='%s', price=%.2f}",
                id, brand, model, year, color, price);
    }
}
```

### Key Active Record Methods (Inherited from ReactivePanacheMongoEntity)

| Method | Type | Description |
|--------|------|-------------|
| `persist()` | Instance | Save new entity to database |
| `update()` | Instance | Update existing entity |
| `delete()` | Instance | Delete entity from database |
| `findById(id)` | Static | Find entity by ID |
| `listAll()` | Static | List all entities (returns `Uni<List<T>>`) |
| `streamAll()` | Static | Stream all entities (returns `Multi<T>`) |
| `find(query, params)` | Static | Custom query with parameters |
| `count()` | Static | Count all documents |
| `deleteAll()` | Static | Delete all documents |

---

## 7. Defining the Protobuf Service

### Step 5: Create the Proto File

Create the file `src/main/proto/car_service.proto`:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "org.acme.grpc";
option java_outer_classname = "CarServiceProto";

package car;

// ============================================================
// Car Service Definition - Demonstrating All 4 gRPC Modes
// ============================================================

service CarService {

    // -------------------- UNARY RPCs --------------------
    
    // Create a new car (Unary: single request, single response)
    rpc CreateCar (CreateCarRequest) returns (CarResponse) {}
    
    // Get a car by ID (Unary)
    rpc GetCar (GetCarRequest) returns (CarResponse) {}
    
    // Update an existing car (Unary)
    rpc UpdateCar (UpdateCarRequest) returns (CarResponse) {}
    
    // Delete a car by ID (Unary)
    rpc DeleteCar (DeleteCarRequest) returns (DeleteCarResponse) {}

    // -------------------- SERVER STREAMING RPC --------------------
    
    // List all cars (Server streams multiple car responses)
    rpc ListCars (ListCarsRequest) returns (stream CarResponse) {}
    
    // Search cars by criteria (Server streams matching cars)
    rpc SearchCars (SearchCarsRequest) returns (stream CarResponse) {}

    // -------------------- CLIENT STREAMING RPC --------------------
    
    // Bulk create cars (Client streams car data, server returns summary)
    rpc BulkCreateCars (stream CreateCarRequest) returns (BulkOperationSummary) {}
    
    // Bulk delete cars (Client streams IDs, server returns summary)
    rpc BulkDeleteCars (stream DeleteCarRequest) returns (BulkOperationSummary) {}

    // -------------------- BIDIRECTIONAL STREAMING RPC --------------------
    
    // Validate cars in real-time (Bidirectional: stream cars, receive validations)
    rpc ValidateCars (stream CreateCarRequest) returns (stream ValidationResponse) {}
    
    // Price quote negotiation (Bidirectional: real-time price adjustments)
    rpc NegotiatePrice (stream PriceNegotiationRequest) returns (stream PriceNegotiationResponse) {}
}

// ============================================================
// Message Definitions
// ============================================================

// ----- Request Messages -----

message CreateCarRequest {
    string brand = 1;
    string model = 2;
    int32 year = 3;
    string color = 4;
    double price = 5;
}

message GetCarRequest {
    string id = 1;
}

message UpdateCarRequest {
    string id = 1;
    string brand = 2;
    string model = 3;
    int32 year = 4;
    string color = 5;
    double price = 6;
}

message DeleteCarRequest {
    string id = 1;
}

message ListCarsRequest {
    // Optional pagination
    int32 page = 1;
    int32 page_size = 2;
}

message SearchCarsRequest {
    string brand = 1;               // Filter by brand
    int32 min_year = 2;             // Minimum year
    int32 max_year = 3;             // Maximum year
    double min_price = 4;           // Minimum price
    double max_price = 5;           // Maximum price
}

message PriceNegotiationRequest {
    string car_id = 1;
    double offered_price = 2;
    string customer_message = 3;
}

// ----- Response Messages -----

message CarResponse {
    string id = 1;
    string brand = 2;
    string model = 3;
    int32 year = 4;
    string color = 5;
    double price = 6;
    int64 created_at = 7;           // Unix timestamp
}

message DeleteCarResponse {
    bool success = 1;
    string message = 2;
}

message BulkOperationSummary {
    int32 total_processed = 1;
    int32 successful = 2;
    int32 failed = 3;
    repeated string error_messages = 4;
}

message ValidationResponse {
    string requested_brand = 1;
    string requested_model = 2;
    bool is_valid = 3;
    repeated string validation_errors = 4;
}

message PriceNegotiationResponse {
    string car_id = 1;
    double original_price = 2;
    double offered_price = 3;
    double counter_offer = 4;
    bool accepted = 5;
    string message = 6;
}
```

### Step 6: Generate Java Classes from Proto

Run Maven to generate the Java classes:

```bash
mvn compile
```

This generates the following in `target/generated-sources/grpc/`:
- Message classes (`CarResponse`, `CreateCarRequest`, etc.)
- Service interface (`CarService`)
- Service stubs for client use

---

## 8. Implementing gRPC Service Methods

### Step 7: Create the gRPC Service Implementation

Create the file `src/main/java/org/acme/service/CarGrpcService.java`:

```java
package org.acme.service;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.entity.Car;
import org.acme.grpc.*;
import org.bson.types.ObjectId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC Service implementation for Car CRUD operations.
 * Demonstrates all four gRPC communication modes.
 */
@GrpcService
public class CarGrpcService implements CarService {

    // ================================================================
    // UNARY RPCs - Single Request, Single Response
    // ================================================================

    /**
     * Creates a new car in the database.
     * 
     * Unary RPC: Client sends one CreateCarRequest, receives one CarResponse.
     */
    @Override
    public Uni<CarResponse> createCar(CreateCarRequest request) {
        Car car = new Car();
        car.brand = request.getBrand();
        car.model = request.getModel();
        car.year = request.getYear();
        car.color = request.getColor();
        car.price = request.getPrice();

        return car.persist()
                .map(ignored -> toCarResponse(car));
    }

    /**
     * Retrieves a car by its ID.
     * 
     * Unary RPC: Client sends GetCarRequest with ID, receives CarResponse.
     */
    @Override
    public Uni<CarResponse> getCar(GetCarRequest request) {
        return Car.<Car>findById(new ObjectId(request.getId()))
                .onItem().ifNull()
                    .failWith(() -> new RuntimeException("Car not found with ID: " + request.getId()))
                .map(this::toCarResponse);
    }

    /**
     * Updates an existing car.
     * 
     * Unary RPC: Client sends UpdateCarRequest, receives updated CarResponse.
     */
    @Override
    public Uni<CarResponse> updateCar(UpdateCarRequest request) {
        return Car.<Car>findById(new ObjectId(request.getId()))
                .onItem().ifNull()
                    .failWith(() -> new RuntimeException("Car not found with ID: " + request.getId()))
                .flatMap(car -> {
                    // Update fields
                    car.brand = request.getBrand();
                    car.model = request.getModel();
                    car.year = request.getYear();
                    car.color = request.getColor();
                    car.price = request.getPrice();
                    
                    return car.update()
                            .map(ignored -> toCarResponse(car));
                });
    }

    /**
     * Deletes a car by its ID.
     * 
     * Unary RPC: Client sends DeleteCarRequest, receives DeleteCarResponse.
     */
    @Override
    public Uni<DeleteCarResponse> deleteCar(DeleteCarRequest request) {
        return Car.<Car>findById(new ObjectId(request.getId()))
                .onItem().ifNull()
                    .failWith(() -> new RuntimeException("Car not found with ID: " + request.getId()))
                .flatMap(car -> car.delete())
                .map(ignored -> DeleteCarResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Car deleted successfully")
                        .build())
                .onFailure().recoverWithItem(throwable -> 
                    DeleteCarResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to delete car: " + throwable.getMessage())
                        .build());
    }

    // ================================================================
    // SERVER STREAMING RPCs - Single Request, Multiple Responses
    // ================================================================

    /**
     * Lists all cars from the database.
     * 
     * Server Streaming RPC: Client sends one request, server streams
     * multiple CarResponse messages back.
     */
    @Override
    public Multi<CarResponse> listCars(ListCarsRequest request) {
        // streamAll() returns a Multi<Car> - reactive stream
        return Car.<Car>streamAll()
                .map(this::toCarResponse);
    }

    /**
     * Searches cars based on criteria.
     * 
     * Server Streaming RPC: Demonstrates complex querying with streaming results.
     */
    @Override
    public Multi<CarResponse> searchCars(SearchCarsRequest request) {
        // Build dynamic query based on provided criteria
        StringBuilder query = new StringBuilder();
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;

        if (!request.getBrand().isEmpty()) {
            query.append("brand = ?").append(paramIndex++);
            params.add(request.getBrand());
        }

        if (request.getMinYear() > 0) {
            if (query.length() > 0) query.append(" and ");
            query.append("year >= ?").append(paramIndex++);
            params.add(request.getMinYear());
        }

        if (request.getMaxYear() > 0) {
            if (query.length() > 0) query.append(" and ");
            query.append("year <= ?").append(paramIndex++);
            params.add(request.getMaxYear());
        }

        if (request.getMinPrice() > 0) {
            if (query.length() > 0) query.append(" and ");
            query.append("price >= ?").append(paramIndex++);
            params.add(request.getMinPrice());
        }

        if (request.getMaxPrice() > 0) {
            if (query.length() > 0) query.append(" and ");
            query.append("price <= ?").append(paramIndex++);
            params.add(request.getMaxPrice());
        }

        // If no criteria provided, return all
        if (query.length() == 0) {
            return Car.<Car>streamAll().map(this::toCarResponse);
        }

        return Car.<Car>find(query.toString(), params.toArray())
                .stream()
                .map(this::toCarResponse);
    }

    // ================================================================
    // CLIENT STREAMING RPCs - Multiple Requests, Single Response
    // ================================================================

    /**
     * Bulk creates multiple cars.
     * 
     * Client Streaming RPC: Client streams multiple CreateCarRequest messages,
     * server processes them and returns a single BulkOperationSummary.
     */
    @Override
    public Uni<BulkOperationSummary> bulkCreateCars(Multi<CreateCarRequest> requests) {
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> errorMessages = new ArrayList<>();

        return requests
                .onItem()
                .transformToUniAndMerge(request -> {
                    Car car = new Car();
                    car.brand = request.getBrand();
                    car.model = request.getModel();
                    car.year = request.getYear();
                    car.color = request.getColor();
                    car.price = request.getPrice();

                    // Validate before persisting
                    if (!car.isValid()) {
                        failed.incrementAndGet();
                        errorMessages.add("Invalid car data: " + request.getBrand() + " " + request.getModel());
                        return Uni.createFrom().voidItem();
                    }

                    return car.persist()
                            .invoke(() -> successful.incrementAndGet())
                            .onFailure().invoke(throwable -> {
                                failed.incrementAndGet();
                                errorMessages.add("Failed to create " + car.brand + " " + car.model + ": " + throwable.getMessage());
                            })
                            .replaceWithVoid();
                })
                .collect().asList()
                .map(ignored -> BulkOperationSummary.newBuilder()
                        .setTotalProcessed(successful.get() + failed.get())
                        .setSuccessful(successful.get())
                        .setFailed(failed.get())
                        .addAllErrorMessages(errorMessages)
                        .build());
    }

    /**
     * Bulk deletes multiple cars by ID.
     * 
     * Client Streaming RPC: Client streams IDs to delete,
     * server returns summary of deletions.
     */
    @Override
    public Uni<BulkOperationSummary> bulkDeleteCars(Multi<DeleteCarRequest> requests) {
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<String> errorMessages = new ArrayList<>();

        return requests
                .onItem()
                .transformToUniAndMerge(request -> {
                    try {
                        ObjectId objectId = new ObjectId(request.getId());
                        return Car.<Car>findById(objectId)
                                .onItem().ifNotNull()
                                    .transformToUni(car -> car.delete()
                                            .invoke(() -> successful.incrementAndGet()))
                                .onItem().ifNull()
                                    .switchTo(() -> {
                                        failed.incrementAndGet();
                                        errorMessages.add("Car not found: " + request.getId());
                                        return Uni.createFrom().voidItem();
                                    })
                                .replaceWithVoid();
                    } catch (IllegalArgumentException e) {
                        failed.incrementAndGet();
                        errorMessages.add("Invalid ID format: " + request.getId());
                        return Uni.createFrom().voidItem();
                    }
                })
                .collect().asList()
                .map(ignored -> BulkOperationSummary.newBuilder()
                        .setTotalProcessed(successful.get() + failed.get())
                        .setSuccessful(successful.get())
                        .setFailed(failed.get())
                        .addAllErrorMessages(errorMessages)
                        .build());
    }

    // ================================================================
    // BIDIRECTIONAL STREAMING RPCs - Multiple Requests, Multiple Responses
    // ================================================================

    /**
     * Validates cars in real-time as they are sent.
     * 
     * Bidirectional Streaming RPC: Client streams car data,
     * server streams back validation results for each car.
     */
    @Override
    public Multi<ValidationResponse> validateCars(Multi<CreateCarRequest> requests) {
        return requests.map(request -> {
            List<String> errors = new ArrayList<>();

            // Validate brand
            if (request.getBrand() == null || request.getBrand().isBlank()) {
                errors.add("Brand is required");
            }

            // Validate model
            if (request.getModel() == null || request.getModel().isBlank()) {
                errors.add("Model is required");
            }

            // Validate year
            if (request.getYear() < 1886) {
                errors.add("Year must be 1886 or later (first car was invented in 1886)");
            } else if (request.getYear() > 2030) {
                errors.add("Year cannot be in the far future");
            }

            // Validate price
            if (request.getPrice() < 0) {
                errors.add("Price cannot be negative");
            } else if (request.getPrice() > 10_000_000) {
                errors.add("Price seems unrealistically high");
            }

            // Validate color
            if (request.getColor() == null || request.getColor().isBlank()) {
                errors.add("Color is recommended");
            }

            return ValidationResponse.newBuilder()
                    .setRequestedBrand(request.getBrand())
                    .setRequestedModel(request.getModel())
                    .setIsValid(errors.isEmpty())
                    .addAllValidationErrors(errors)
                    .build();
        });
    }

    /**
     * Simulates a price negotiation process.
     * 
     * Bidirectional Streaming RPC: Client sends price offers,
     * server responds with counter-offers or acceptance.
     */
    @Override
    public Multi<PriceNegotiationResponse> negotiatePrice(Multi<PriceNegotiationRequest> requests) {
        return requests
                .onItem()
                .transformToUniAndMerge(request -> {
                    // Find the car to get original price
                    return Car.<Car>findById(new ObjectId(request.getCarId()))
                            .map(car -> {
                                if (car == null) {
                                    return PriceNegotiationResponse.newBuilder()
                                            .setCarId(request.getCarId())
                                            .setMessage("Car not found")
                                            .setAccepted(false)
                                            .build();
                                }

                                double originalPrice = car.price;
                                double offeredPrice = request.getOfferedPrice();
                                double minimumAcceptable = originalPrice * 0.85; // 15% discount max

                                if (offeredPrice >= originalPrice) {
                                    // Full price or above - accept immediately
                                    return PriceNegotiationResponse.newBuilder()
                                            .setCarId(request.getCarId())
                                            .setOriginalPrice(originalPrice)
                                            .setOfferedPrice(offeredPrice)
                                            .setCounterOffer(offeredPrice)
                                            .setAccepted(true)
                                            .setMessage("Offer accepted! Great choice!")
                                            .build();
                                } else if (offeredPrice >= minimumAcceptable) {
                                    // Within acceptable range - accept
                                    return PriceNegotiationResponse.newBuilder()
                                            .setCarId(request.getCarId())
                                            .setOriginalPrice(originalPrice)
                                            .setOfferedPrice(offeredPrice)
                                            .setCounterOffer(offeredPrice)
                                            .setAccepted(true)
                                            .setMessage("Deal! You negotiated well.")
                                            .build();
                                } else {
                                    // Too low - counter offer
                                    double counterOffer = (offeredPrice + minimumAcceptable) / 2;
                                    counterOffer = Math.max(counterOffer, minimumAcceptable);
                                    
                                    return PriceNegotiationResponse.newBuilder()
                                            .setCarId(request.getCarId())
                                            .setOriginalPrice(originalPrice)
                                            .setOfferedPrice(offeredPrice)
                                            .setCounterOffer(counterOffer)
                                            .setAccepted(false)
                                            .setMessage("That's too low. How about " + String.format("%.2f", counterOffer) + "?")
                                            .build();
                                }
                            })
                            .onFailure().recoverWithItem(throwable ->
                                    PriceNegotiationResponse.newBuilder()
                                            .setCarId(request.getCarId())
                                            .setMessage("Error: " + throwable.getMessage())
                                            .setAccepted(false)
                                            .build());
                });
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    /**
     * Converts a Car entity to a CarResponse protobuf message.
     */
    private CarResponse toCarResponse(Car car) {
        return CarResponse.newBuilder()
                .setId(car.id.toString())
                .setBrand(car.brand)
                .setModel(car.model)
                .setYear(car.year)
                .setColor(car.color != null ? car.color : "")
                .setPrice(car.price)
                .setCreatedAt(Instant.now().toEpochMilli())
                .build();
    }
}
```

---

## 9. Error Handling and Interceptors

### Custom Exception Handling

Create a custom exception handler for better error responses:

`src/main/java/org/acme/exception/CarExceptionHandlerProvider.java`:

```java
package org.acme.exception;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.runtime.exception.ExceptionHandler;
import io.quarkus.grpc.runtime.exception.ExceptionHandlerProvider;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Custom exception handler for gRPC service errors.
 */
@ApplicationScoped
public class CarExceptionHandlerProvider implements ExceptionHandlerProvider {

    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(
            ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata) {
        return new CarExceptionHandler<>(listener, serverCall, metadata);
    }

    @Override
    public Throwable transform(Throwable t) {
        // Transform known exceptions to appropriate gRPC statuses
        if (t instanceof CarNotFoundException cnf) {
            return new StatusRuntimeException(
                    Status.NOT_FOUND.withDescription(cnf.getMessage()));
        } else if (t instanceof InvalidCarDataException icd) {
            return new StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(icd.getMessage()));
        } else if (t instanceof IllegalArgumentException iae) {
            return new StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription("Invalid argument: " + iae.getMessage()));
        }
        // Default handling
        return ExceptionHandlerProvider.toStatusException(t, true);
    }

    private static class CarExceptionHandler<A, B> extends ExceptionHandler<A, B> {
        public CarExceptionHandler(ServerCall.Listener<A> listener, 
                                   ServerCall<A, B> call, 
                                   Metadata metadata) {
            super(listener, call, metadata);
        }

        @Override
        protected void handleException(Throwable t, ServerCall<A, B> call, Metadata metadata) {
            StatusRuntimeException sre = (StatusRuntimeException) 
                    ExceptionHandlerProvider.toStatusException(t, true);
            Metadata trailers = sre.getTrailers() != null ? sre.getTrailers() : metadata;
            call.close(sre.getStatus(), trailers);
        }
    }
}
```

Create custom exception classes:

`src/main/java/org/acme/exception/CarNotFoundException.java`:

```java
package org.acme.exception;

public class CarNotFoundException extends RuntimeException {
    public CarNotFoundException(String id) {
        super("Car not found with ID: " + id);
    }
}
```

`src/main/java/org/acme/exception/InvalidCarDataException.java`:

```java
package org.acme.exception;

public class InvalidCarDataException extends RuntimeException {
    public InvalidCarDataException(String message) {
        super("Invalid car data: " + message);
    }
}
```

### Server Interceptors

Create a logging interceptor to monitor all gRPC calls:

`src/main/java/org/acme/interceptor/LoggingInterceptor.java`:

```java
package org.acme.interceptor;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Global interceptor that logs all gRPC calls.
 */
@ApplicationScoped
@GlobalInterceptor
public class LoggingInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(LoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = System.currentTimeMillis();
        
        LOG.infof(">>> gRPC Call Started: %s", methodName);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(
                        new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                            @Override
                            public void close(Status status, Metadata trailers) {
                                long duration = System.currentTimeMillis() - startTime;
                                LOG.infof("<<< gRPC Call Completed: %s | Status: %s | Duration: %dms",
                                        methodName, status.getCode(), duration);
                                super.close(status, trailers);
                            }
                        },
                        headers)) {
            
            @Override
            public void onMessage(ReqT message) {
                LOG.debugf("    Received message for %s", methodName);
                super.onMessage(message);
            }
        };
    }
}
```

---

## 10. Testing Your gRPC Server

### Step 8: Configure the Application

Create or update `src/main/resources/application.properties`:

```properties
# ============================================================
# MongoDB Configuration
# ============================================================
quarkus.mongodb.connection-string=mongodb://localhost:27017
quarkus.mongodb.database=cardb

# ============================================================
# gRPC Server Configuration
# ============================================================

# Server port (default is 9000)
quarkus.grpc.server.port=9000

# Enable gRPC server reflection (useful for tools like grpcurl)
quarkus.grpc.server.enable-reflection-service=true

# Use plaintext (no TLS) for development
quarkus.grpc.server.plain-text=true

# ============================================================
# Logging Configuration
# ============================================================
quarkus.log.level=INFO
quarkus.log.category."org.acme".level=DEBUG
quarkus.log.category."io.grpc".level=INFO

# ============================================================
# Dev Services (Auto-start MongoDB in dev mode)
# ============================================================
quarkus.mongodb.devservices.enabled=true
quarkus.mongodb.devservices.image-name=mongo:6.0
```

### Step 9: Start MongoDB (for Production/Manual Testing)

```bash
# Using Docker
docker run -d --name mongodb -p 27017:27017 mongo:6.0
```

> **Note:** In dev mode (`quarkus dev`), Quarkus automatically starts a MongoDB container via Dev Services!

### Step 10: Run the Application

```bash
# Dev mode with live reload
mvn quarkus:dev

# Or using Quarkus CLI
quarkus dev
```

You should see output like:
```
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
INFO  [io.quarkus] (main) car-grpc-service 1.0-SNAPSHOT started in 2.345s.
INFO  [io.quarkus] (main) Profile dev activated. Live Coding activated.
INFO  [io.quarkus] (main) gRPC Server started on 0.0.0.0:9000
```

### Step 11: Test with grpcurl

Install `grpcurl` if you haven't already:

```bash
# macOS
brew install grpcurl

# Linux (download from releases)
# https://github.com/fullstorydev/grpcurl/releases
```

**Test Unary - Create Car:**
```bash
grpcurl -plaintext \
  -d '{"brand": "Tesla", "model": "Model 3", "year": 2024, "color": "Red", "price": 45000}' \
  localhost:9000 car.CarService/CreateCar
```

**Expected Response:**
```json
{
  "id": "65abc123def456789012345",
  "brand": "Tesla",
  "model": "Model 3",
  "year": 2024,
  "color": "Red",
  "price": 45000,
  "createdAt": "1706086800000"
}
```

**Test Unary - Get Car:**
```bash
grpcurl -plaintext \
  -d '{"id": "65abc123def456789012345"}' \
  localhost:9000 car.CarService/GetCar
```

**Test Server Streaming - List All Cars:**
```bash
grpcurl -plaintext \
  -d '{}' \
  localhost:9000 car.CarService/ListCars
```

**Test Server Streaming - Search Cars:**
```bash
grpcurl -plaintext \
  -d '{"brand": "Tesla", "minYear": 2020, "maxPrice": 60000}' \
  localhost:9000 car.CarService/SearchCars
```

**Test Bidirectional Streaming - Validate Cars:**
```bash
# Interactive mode - type each message, press Enter
grpcurl -plaintext \
  localhost:9000 car.CarService/ValidateCars <<EOF
{"brand": "Toyota", "model": "Camry", "year": 2023, "price": 30000}
{"brand": "", "model": "Test", "year": 1800, "price": -100}
EOF
```

**List Available Services (reflection):**
```bash
grpcurl -plaintext localhost:9000 list
```

**Describe a Service:**
```bash
grpcurl -plaintext localhost:9000 describe car.CarService
```

---

## 11. Configuration Reference

### gRPC Server Properties

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.grpc.server.port` | `9000` | The gRPC server port |
| `quarkus.grpc.server.host` | `0.0.0.0` | Interface to bind to |
| `quarkus.grpc.server.plain-text` | `true` (dev) | Use plaintext (no TLS) |
| `quarkus.grpc.server.enable-reflection-service` | `false` | Enable gRPC reflection |
| `quarkus.grpc.server.max-inbound-message-size` | | Maximum incoming message size |
| `quarkus.grpc.server.max-inbound-metadata-size` | | Maximum incoming metadata size |
| `quarkus.grpc.server.handshake-timeout` | | Handshake timeout duration |
| `quarkus.grpc.server.compression` | | Default compression (gzip, deflate, identity) |

### TLS/SSL Configuration

For production deployments:

```properties
# Disable plain text
quarkus.grpc.server.plain-text=false

# Certificate and key paths
quarkus.grpc.server.ssl.certificate=server.crt
quarkus.grpc.server.ssl.key=server.key

# Or use a keystore
quarkus.grpc.server.ssl.key-store=keystore.jks
quarkus.grpc.server.ssl.key-store-password=secret
```

### MongoDB Configuration

| Property | Description |
|----------|-------------|
| `quarkus.mongodb.connection-string` | MongoDB connection URI |
| `quarkus.mongodb.database` | Default database name |
| `quarkus.mongodb.credentials.username` | Authentication username |
| `quarkus.mongodb.credentials.password` | Authentication password |
| `quarkus.mongodb.devservices.enabled` | Auto-start MongoDB in dev mode |

---

## 12. Useful Resources for Continued Learning

### Official Documentation

| Resource | Description | Link |
|----------|-------------|------|
| **Quarkus gRPC Guide** | Official getting started guide for gRPC with Quarkus | [quarkus.io/guides/grpc-getting-started](https://quarkus.io/guides/grpc-getting-started) |
| **Quarkus gRPC Service Implementation** | In-depth guide for implementing gRPC services | [quarkus.io/guides/grpc-service-implementation](https://quarkus.io/guides/grpc-service-implementation) |
| **Quarkus gRPC Service Consumption** | Guide for consuming gRPC services as a client | [quarkus.io/guides/grpc-service-consumption](https://quarkus.io/guides/grpc-service-consumption) |
| **Quarkus MongoDB Panache Guide** | Official MongoDB with Panache documentation | [quarkus.io/guides/mongodb-panache](https://quarkus.io/guides/mongodb-panache) |
| **gRPC Official Documentation** | Core gRPC concepts and documentation | [grpc.io/docs](https://grpc.io/docs/) |
| **Protocol Buffers Documentation** | Official Protobuf language guide | [protobuf.dev](https://protobuf.dev/) |

### Video Tutorials

| Resource | Description |
|----------|-------------|
| **Quarkus YouTube Channel** | Official Quarkus videos including gRPC tutorials |
| **Devoxx Presentations** | Conference talks on Quarkus and gRPC |

### Books and Articles

| Resource | Description |
|----------|-------------|
| **gRPC Up and Running** (O'Reilly) | Comprehensive book on gRPC patterns and practices |
| **Quarkus in Action** (Manning) | Book covering Quarkus development including gRPC |
| **Piotr Minkowski's Blog** | Excellent articles on Quarkus microservices |

### Tools

| Tool | Description | Link |
|------|-------------|------|
| **grpcurl** | Command-line tool for testing gRPC services | [github.com/fullstorydev/grpcurl](https://github.com/fullstorydev/grpcurl) |
| **BloomRPC** | Desktop GUI client for gRPC | [github.com/bloomrpc/bloomrpc](https://github.com/bloomrpc/bloomrpc) |
| **Postman** | API testing tool with gRPC support | [postman.com](https://www.postman.com/) |
| **Evans** | More expressive universal gRPC client | [github.com/ktr0731/evans](https://github.com/ktr0731/evans) |
| **gRPC-Gateway** | Generate RESTful JSON API from gRPC | [github.com/grpc-ecosystem/grpc-gateway](https://github.com/grpc-ecosystem/grpc-gateway) |

### Community Resources

| Resource | Description |
|----------|-------------|
| **Quarkus Zulip Chat** | Active community chat for Quarkus developers |
| **Stack Overflow** | Tag: `quarkus` + `grpc` for Q&A |
| **GitHub Discussions** | Quarkus repository discussions |
| **Quarkiverse** | Community extensions for Quarkus |

### SmallRye Mutiny (Reactive Programming)

| Resource | Description | Link |
|----------|-------------|------|
| **Mutiny Documentation** | Official Mutiny reactive library docs | [smallrye.io/smallrye-mutiny](https://smallrye.io/smallrye-mutiny/) |
| **Mutiny Getting Started** | Quick start guide for Mutiny | [smallrye.io/smallrye-mutiny/getting-started](https://smallrye.io/smallrye-mutiny/getting-started) |

---

## Summary

Congratulations! 🎉 You've learned how to build a comprehensive gRPC server with Quarkus featuring:

✅ **All four gRPC communication modes:**
- Unary RPC for simple request-response
- Server Streaming for returning multiple results
- Client Streaming for bulk operations
- Bidirectional Streaming for real-time interactions

✅ **MongoDB integration:**
- Active Record pattern with Panache
- Reactive database operations with Mutiny
- Custom query methods

✅ **Production-ready features:**
- Custom exception handling
- Logging interceptors
- Proper configuration

✅ **Testing strategies:**
- Using grpcurl for command-line testing
- Understanding gRPC reflection

The combination of Quarkus's reactive-first design with gRPC's efficient binary protocol creates high-performance microservices that are ready for cloud-native deployment.

Happy coding! 🚀

---

*This guide was created to help developers get started with gRPC in Quarkus. For the most up-to-date information, always refer to the official Quarkus documentation.*
