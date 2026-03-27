# Quarkus Reactive MongoDB with Panache - Comprehensive Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Understanding Reactive Programming with Panache](#understanding-reactive-programming-with-panache)
3. [Project Setup and Configuration](#project-setup-and-configuration)
4. [Active Record Pattern](#active-record-pattern)
5. [Repository Pattern](#repository-pattern)
6. [Reactive CRUD Operations](#reactive-crud-operations)
7. [Advanced Querying](#advanced-querying)
8. [Reactive Transactions](#reactive-transactions)
9. [Testing Reactive MongoDB Panache](#testing-reactive-mongodb-panache)
10. [Best Practices](#best-practices)
11. [Useful Resources](#useful-resources)

---

## Introduction

Welcome to the comprehensive guide for **Reactive MongoDB programming with Quarkus using Panache**! This guide is designed for developers who want to leverage the power of reactive programming when working with MongoDB in the Quarkus framework.

**Quarkus** is a Kubernetes-native Java framework tailored for GraalVM and HotSpot, offering supersonic startup times and incredibly low memory footprint. When combined with **Panache**, MongoDB operations become significantly simplified, reducing boilerplate code while maintaining the flexibility needed for complex applications.

### What You Will Learn

- Fundamental concepts of reactive programming with MongoDB and Panache
- How to configure a Quarkus project for reactive MongoDB
- Two approaches: **Active Record Pattern** and **Repository Pattern**
- Complete CRUD operations using reactive types
- Testing strategies for reactive MongoDB entities
- Best practices for production-ready applications

---

## Understanding Reactive Programming with Panache

### What is Panache?

**Panache** is a Quarkus extension that provides a simplified, opinionated approach to database persistence. It reduces the boilerplate code needed to interact with databases by providing:

- **Static methods** for common operations (Active Record pattern)
- **Repository abstractions** for separating data access logic (Repository pattern)
- **Simplified query language** (PanacheQL) similar to JPQL/HQL
- **Built-in pagination, sorting, and streaming capabilities**

### What is Reactive MongoDB with Panache?

Reactive MongoDB with Panache combines the simplicity of Panache with the power of **reactive programming**. Instead of blocking I/O operations, reactive variants use **non-blocking** operations that are ideal for:

- High-throughput applications
- Microservices architectures
- Event-driven systems
- Applications requiring efficient resource utilization

### Mutiny: The Reactive Foundation

Quarkus uses **Mutiny** as its reactive programming library. Mutiny provides two main types:

| Type | Description | Use Case |
|------|-------------|----------|
| `Uni<T>` | Represents an asynchronous action that emits a single item (or nothing) | Single value operations (findById, persist, count) |
| `Multi<T>` | Represents a stream of items | Multiple value operations (streaming all entities) |

**Key Characteristics of Mutiny Types:**

```java
// Uni<T> - emits 0 or 1 item, then completes
Uni<Person> person = Person.findById(id);

// Multi<T> - emits 0 to n items, then completes
Multi<Person> persons = Person.streamAll();
```

### Reactive vs Imperative: Key Differences

| Aspect | Imperative | Reactive |
|--------|------------|----------|
| **Base Entity Class** | `PanacheMongoEntity` | `ReactivePanacheMongoEntity` |
| **Base Repository** | `PanacheMongoRepository` | `ReactivePanacheMongoRepository` |
| **Return Types** | `T`, `List<T>`, `Optional<T>` | `Uni<T>`, `Uni<List<T>>`, `Uni<Optional<T>>` |
| **Streaming** | `Stream<T>` | `Multi<T>` |
| **Execution Model** | Blocking I/O | Non-blocking I/O |

---

## Project Setup and Configuration

### Step 1: Create a New Quarkus Project

You can create a new Quarkus project using the Quarkus CLI or Maven:

**Using Quarkus CLI:**

```bash
quarkus create app org.acme:mongodb-reactive-demo \
    --extension='rest-jackson,mongodb-panache' \
    --no-code
cd mongodb-reactive-demo
```

**Using Maven:**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.0:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=mongodb-reactive-demo \
    -Dextensions='rest-jackson,mongodb-panache' \
    -DnoCode
cd mongodb-reactive-demo
```

### Step 2: Add Required Dependencies

If adding to an existing project, ensure you have the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-mongodb-panache</artifactId>
</dependency>
```

**For Gradle:**

```groovy
implementation("io.quarkus:quarkus-mongodb-panache")
```

### Step 3: Configure MongoDB Connection

Add the MongoDB configuration to your `application.properties`:

```properties
# MongoDB Configuration
# Configure the MongoDB client for a single node
quarkus.mongodb.connection-string=mongodb://localhost:27017

# Mandatory: specify the database name
quarkus.mongodb.database=myapp

# Optional: Configure connection pool
quarkus.mongodb.min-pool-size=5
quarkus.mongodb.max-pool-size=50

# Optional: Enable query logging for debugging
quarkus.log.category."io.quarkus.mongodb.panache.common.reactive.runtime".level=DEBUG
```

**For Replica Set Configuration:**

```properties
# Configure for a replica set (required for transactions)
quarkus.mongodb.connection-string=mongodb://mongo1:27017,mongo2:27017,mongo3:27017/?replicaSet=rs0
quarkus.mongodb.database=myapp
```

### Step 4: Use Quarkus Dev Services (Recommended for Development)

Quarkus provides **Dev Services** that automatically start a MongoDB container during development and testing. This is the easiest way to get started!

Simply remove the connection string from your development configuration, and Quarkus will automatically provision a MongoDB container:

```properties
# application.properties (Development profile)
%dev.quarkus.mongodb.database=myapp
# No connection-string needed - Dev Services will handle it!

# For production, specify the connection string
%prod.quarkus.mongodb.connection-string=mongodb://production-host:27017
%prod.quarkus.mongodb.database=myapp
```

Dev Services will:
- Automatically start a MongoDB container using Testcontainers
- Configure the connection string automatically
- Set up a single-node replica set (enabling transactions)
- Persist data between restarts (unless configured otherwise)

---

## Active Record Pattern

The **Active Record pattern** embeds persistence operations directly into your entity class. This approach is ideal for simpler applications where entities can manage their own persistence.

### Defining a Reactive Entity

To create a reactive entity using the Active Record pattern, extend `ReactivePanacheMongoEntity`:

```java
package org.acme.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.time.LocalDate;

@MongoEntity(collection = "persons", database = "myapp")
public class Person extends ReactivePanacheMongoEntity {
    
    // Public fields - Panache will generate getters/setters
    public String name;
    
    @BsonProperty("birth_date")  // Custom field name in MongoDB
    public LocalDate birthDate;
    
    public Status status;
    
    @BsonIgnore  // This field won't be persisted
    public transient String temporaryData;
    
    // Custom accessor - automatically used when accessing 'name'
    public String getName() {
        return name != null ? name.toUpperCase() : null;
    }
    
    // Custom mutator - automatically used when setting 'name'
    public void setName(String name) {
        this.name = name != null ? name.toLowerCase() : null;
    }
}
```

### Status Enum

```java
package org.acme.entity;

public enum Status {
    ALIVE, DECEASED, UNKNOWN
}
```

### Adding Custom Query Methods

One of the most powerful features of the Active Record pattern is the ability to add custom static query methods:

```java
@MongoEntity(collection = "persons")
public class Person extends ReactivePanacheMongoEntity {
    
    public String name;
    public LocalDate birthDate;
    public Status status;
    public String email;
    public Integer age;
    
    // Custom finder methods
    public static Uni<Person> findByName(String name) {
        return find("name", name).firstResult();
    }
    
    public static Uni<List<Person>> findAlive() {
        return list("status", Status.ALIVE);
    }
    
    public static Uni<List<Person>> findByAgeGreaterThan(int minAge) {
        return list("age > ?1", minAge);
    }
    
    public static Uni<List<Person>> findByNameAndStatus(String name, Status status) {
        return list("name = ?1 and status = ?2", name, status);
    }
    
    public static Uni<Optional<Person>> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
    
    // Custom delete method
    public static Uni<Long> deleteByStatus(Status status) {
        return delete("status", status);
    }
    
    // Streaming method for large datasets
    public static Multi<Person> streamAlive() {
        return stream("status", Status.ALIVE);
    }
}
```

### Using Custom ID

By default, `ReactivePanacheMongoEntity` uses `ObjectId` as the ID type. To use a custom ID, extend `ReactivePanacheMongoEntityBase`:

```java
package org.acme.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import org.bson.codecs.pojo.annotations.BsonId;

@MongoEntity(collection = "products")
public class Product extends ReactivePanacheMongoEntityBase {
    
    @BsonId
    public String sku;  // Use SKU as the custom ID
    
    public String name;
    public Double price;
    public Integer quantity;
}
```

---

## Repository Pattern

The **Repository pattern** separates data access logic from your entity classes, providing better separation of concerns. This is the recommended approach for larger, more complex applications.

### Defining a Plain Entity

When using the Repository pattern, your entity is a simple POJO:

```java
package org.acme.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDate;

@MongoEntity(collection = "persons", database = "myapp")
public class Person {
    
    public ObjectId id;  // MongoDB _id field
    
    public String name;
    
    @BsonProperty("birth_date")
    public LocalDate birthDate;
    
    public Status status;
    
    public String email;
    
    public Integer age;
    
    // Constructors
    public Person() {
    }
    
    public Person(String name, LocalDate birthDate, Status status) {
        this.name = name;
        this.birthDate = birthDate;
        this.status = status;
    }
}
```

### Defining a Reactive Repository

To create a reactive repository, implement `ReactivePanacheMongoRepository`:

```java
package org.acme.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.Person;
import org.acme.entity.Status;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PersonRepository implements ReactivePanacheMongoRepository<Person> {
    
    // Custom finder methods
    public Uni<Person> findByName(String name) {
        return find("name", name).firstResult();
    }
    
    public Uni<List<Person>> findAlive() {
        return list("status", Status.ALIVE);
    }
    
    public Uni<List<Person>> findByAgeGreaterThan(int minAge) {
        return list("age > ?1", minAge);
    }
    
    public Uni<Optional<Person>> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
    
    // Custom delete method
    public Uni<Long> deleteByStatus(Status status) {
        return delete("status", status);
    }
    
    // Streaming method
    public Multi<Person> streamAlive() {
        return stream("status", Status.ALIVE);
    }
    
    // Bulk update method
    public Uni<Long> updateStatusByAge(Status newStatus, int minAge) {
        return update("status", newStatus).where("age > ?1", minAge);
    }
}
```

### Using Custom ID with Repository Pattern

For custom IDs, implement `ReactivePanacheMongoRepositoryBase`:

```java
package org.acme.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.Product;

@ApplicationScoped
public class ProductRepository implements ReactivePanacheMongoRepositoryBase<Product, String> {
    
    public Uni<Product> findBySku(String sku) {
        return findById(sku);
    }
    
    public Uni<List<Product>> findByPriceRange(double minPrice, double maxPrice) {
        return list("price >= ?1 and price <= ?2", minPrice, maxPrice);
    }
}
```

---

## Reactive CRUD Operations

### CRUD with Active Record Pattern

```java
package org.acme.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.Person;
import org.acme.entity.Status;
import org.bson.types.ObjectId;

import java.net.URI;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@ApplicationScoped
public class PersonServiceActiveRecord {
    
    // ============== CREATE ==============
    
    /**
     * Persist a single entity
     */
    public Uni<Person> createPerson(String name, LocalDate birthDate) {
        Person person = new Person();
        person.name = name;
        person.birthDate = birthDate;
        person.status = Status.ALIVE;
        
        // persist() returns Uni<Person> with the populated ID
        return person.persist();
    }
    
    /**
     * Persist multiple entities
     */
    public Uni<Void> createPersons(List<Person> persons) {
        return Person.persist(persons);
    }
    
    /**
     * Persist or Update (upsert)
     */
    public Uni<Person> saveOrUpdatePerson(Person person) {
        return person.persistOrUpdate();
    }
    
    // ============== READ ==============
    
    /**
     * Find by ID
     */
    public Uni<Person> findById(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return Person.findById(id);
    }
    
    /**
     * Find by ID with Optional
     */
    public Uni<Person> findByIdOrThrow(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return Person.findByIdOptional(id)
                .onItem().ifNull().failWith(() -> 
                    new IllegalArgumentException("Person not found: " + idAsString));
    }
    
    /**
     * Find all entities
     */
    public Uni<List<Person>> findAll() {
        return Person.listAll();
    }
    
    /**
     * Find with query
     */
    public Uni<List<Person>> findByStatus(Status status) {
        return Person.list("status", status);
    }
    
    /**
     * Count entities
     */
    public Uni<Long> countAll() {
        return Person.count();
    }
    
    /**
     * Count with query
     */
    public Uni<Long> countByStatus(Status status) {
        return Person.count("status", status);
    }
    
    /**
     * Stream all entities (for large datasets)
     */
    public Multi<Person> streamAll() {
        return Person.streamAll();
    }
    
    // ============== UPDATE ==============
    
    /**
     * Update a single entity
     */
    public Uni<Person> updatePerson(String idAsString, Status newStatus) {
        ObjectId id = new ObjectId(idAsString);
        return Person.<Person>findById(id)
                .onItem().ifNotNull().transformToUni(person -> {
                    person.status = newStatus;
                    return person.update();
                });
    }
    
    /**
     * Bulk update
     */
    public Uni<Long> updateStatusForAll(Status newStatus) {
        return Person.update("status", newStatus).all();
    }
    
    /**
     * Bulk update with condition
     */
    public Uni<Long> setStatusForAge(Status newStatus, int minAge) {
        return Person.update("status", newStatus).where("age > ?1", minAge);
    }
    
    // ============== DELETE ==============
    
    /**
     * Delete a single entity
     */
    public Uni<Void> deletePerson(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return Person.<Person>findById(id)
                .onItem().ifNotNull().transformToUni(person -> person.delete());
    }
    
    /**
     * Delete by ID
     */
    public Uni<Boolean> deleteById(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return Person.deleteById(id);
    }
    
    /**
     * Delete with condition
     */
    public Uni<Long> deleteByStatus(Status status) {
        return Person.delete("status", status);
    }
    
    /**
     * Delete all
     */
    public Uni<Long> deleteAll() {
        return Person.deleteAll();
    }
}
```

### CRUD with Repository Pattern

```java
package org.acme.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.entity.Person;
import org.acme.entity.Status;
import org.acme.repository.PersonRepository;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PersonServiceRepository {
    
    @Inject
    PersonRepository personRepository;
    
    // ============== CREATE ==============
    
    /**
     * Persist a single entity
     */
    public Uni<Person> createPerson(String name, LocalDate birthDate) {
        Person person = new Person();
        person.name = name;
        person.birthDate = birthDate;
        person.status = Status.ALIVE;
        
        return personRepository.persist(person)
                .replaceWith(person);
    }
    
    /**
     * Persist multiple entities
     */
    public Uni<Void> createPersons(List<Person> persons) {
        return personRepository.persist(persons);
    }
    
    /**
     * Persist or Update (upsert)
     */
    public Uni<Void> saveOrUpdatePerson(Person person) {
        return personRepository.persistOrUpdate(person);
    }
    
    // ============== READ ==============
    
    /**
     * Find by ID
     */
    public Uni<Person> findById(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return personRepository.findById(id);
    }
    
    /**
     * Find by ID with Optional
     */
    public Uni<Person> findByIdOrThrow(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return personRepository.findByIdOptional(id)
                .onItem().ifNull().failWith(() -> 
                    new IllegalArgumentException("Person not found: " + idAsString));
    }
    
    /**
     * Find all entities
     */
    public Uni<List<Person>> findAll() {
        return personRepository.listAll();
    }
    
    /**
     * Find with query
     */
    public Uni<List<Person>> findByStatus(Status status) {
        return personRepository.list("status", status);
    }
    
    /**
     * Count entities
     */
    public Uni<Long> countAll() {
        return personRepository.count();
    }
    
    /**
     * Count with query
     */
    public Uni<Long> countByStatus(Status status) {
        return personRepository.count("status", status);
    }
    
    /**
     * Stream all entities (for large datasets)
     */
    public Multi<Person> streamAll() {
        return personRepository.streamAll();
    }
    
    // ============== UPDATE ==============
    
    /**
     * Update a single entity
     */
    public Uni<Person> updatePerson(String idAsString, Status newStatus) {
        ObjectId id = new ObjectId(idAsString);
        return personRepository.findById(id)
                .onItem().ifNotNull().transformToUni(person -> {
                    person.status = newStatus;
                    return personRepository.update(person).replaceWith(person);
                });
    }
    
    /**
     * Bulk update
     */
    public Uni<Long> updateStatusForAll(Status newStatus) {
        return personRepository.update("status", newStatus).all();
    }
    
    /**
     * Bulk update with condition
     */
    public Uni<Long> setStatusForAge(Status newStatus, int minAge) {
        return personRepository.update("status", newStatus).where("age > ?1", minAge);
    }
    
    // ============== DELETE ==============
    
    /**
     * Delete a single entity
     */
    public Uni<Void> deletePerson(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return personRepository.findById(id)
                .onItem().ifNotNull().transformToUni(personRepository::delete);
    }
    
    /**
     * Delete by ID
     */
    public Uni<Boolean> deleteById(String idAsString) {
        ObjectId id = new ObjectId(idAsString);
        return personRepository.deleteById(id);
    }
    
    /**
     * Delete with condition
     */
    public Uni<Long> deleteByStatus(Status status) {
        return personRepository.delete("status", status);
    }
    
    /**
     * Delete all
     */
    public Uni<Long> deleteAll() {
        return personRepository.deleteAll();
    }
}
```

### REST Resource Example

Here's a complete REST resource demonstrating reactive MongoDB operations:

```java
package org.acme.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.entity.Person;
import org.acme.entity.Status;
import org.acme.repository.PersonRepository;
import org.bson.types.ObjectId;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.net.URI;
import java.util.List;

@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonResource {
    
    @Inject
    PersonRepository personRepository;
    
    @GET
    public Uni<List<Person>> getAllPersons() {
        return personRepository.listAll();
    }
    
    @GET
    @Path("/{id}")
    public Uni<Response> getPersonById(@PathParam("id") String id) {
        return personRepository.findById(new ObjectId(id))
                .onItem().ifNotNull().transform(person -> Response.ok(person).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Person> streamAllPersons() {
        return personRepository.streamAll();
    }
    
    @GET
    @Path("/status/{status}")
    public Uni<List<Person>> getPersonsByStatus(@PathParam("status") Status status) {
        return personRepository.list("status", status);
    }
    
    @POST
    public Uni<Response> createPerson(Person person) {
        return personRepository.persist(person)
                .replaceWith(() -> 
                    Response.created(URI.create("/persons/" + person.id))
                            .entity(person)
                            .build());
    }
    
    @PUT
    @Path("/{id}")
    public Uni<Response> updatePerson(@PathParam("id") String id, Person updatedPerson) {
        return personRepository.findById(new ObjectId(id))
                .onItem().ifNotNull().transformToUni(existingPerson -> {
                    existingPerson.name = updatedPerson.name;
                    existingPerson.status = updatedPerson.status;
                    existingPerson.birthDate = updatedPerson.birthDate;
                    return personRepository.update(existingPerson)
                            .replaceWith(Response.ok(existingPerson).build());
                })
                .onItem().ifNull().continueWith(
                    Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @DELETE
    @Path("/{id}")
    public Uni<Response> deletePerson(@PathParam("id") String id) {
        return personRepository.deleteById(new ObjectId(id))
                .map(deleted -> deleted 
                    ? Response.noContent().build()
                    : Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @GET
    @Path("/count")
    public Uni<Long> countPersons() {
        return personRepository.count();
    }
}
```

---

## Advanced Querying

### PanacheQL: Simplified Query Language

Panache supports **PanacheQL**, a simplified query language similar to JPQL/HQL. Queries that don't start with `{` are automatically treated as PanacheQL and converted to MongoDB native queries.

#### Comparison Operators

```java
// Equals
Uni<List<Person>> result = Person.list("name", "John");
// Converts to: {'name': 'John'}

// Not equals
Uni<List<Person>> result = Person.list("name != ?1", "John");
// Converts to: {'name': {'$ne': 'John'}}

// Greater than
Uni<List<Person>> result = Person.list("age > ?1", 21);
// Converts to: {'age': {'$gt': 21}}

// Greater than or equal
Uni<List<Person>> result = Person.list("age >= ?1", 18);
// Converts to: {'age': {'$gte': 18}}

// Less than
Uni<List<Person>> result = Person.list("age < ?1", 65);
// Converts to: {'age': {'$lt': 65}}

// Less than or equal
Uni<List<Person>> result = Person.list("age <= ?1", 30);
// Converts to: {'age': {'$lte': 30}}
```

#### Logical Operators

```java
// AND
Uni<List<Person>> result = Person.list("name = ?1 and status = ?2", "John", Status.ALIVE);
// Converts to: {'name': 'John', 'status': 'ALIVE'}

// OR
Uni<List<Person>> result = Person.list("status = ?1 or status = ?2", Status.ALIVE, Status.UNKNOWN);
// Converts to: {'$or': [{'status': 'ALIVE'}, {'status': 'UNKNOWN'}]}
```

#### Special Operators

```java
// IS NULL
Uni<List<Person>> result = Person.list("email is null");
// Converts to: {'email': {'$exists': false}}

// IS NOT NULL
Uni<List<Person>> result = Person.list("email is not null");
// Converts to: {'email': {'$exists': true}}

// LIKE (uses MongoDB $regex)
Uni<List<Person>> result = Person.list("name like ?1", "^John");
// Converts to: {'name': {'$regex': '^John'}}

// IN
List<Status> statuses = List.of(Status.ALIVE, Status.UNKNOWN);
Uni<List<Person>> result = Person.list("status in ?1", statuses);
// Converts to: {'status': {'$in': ['ALIVE', 'UNKNOWN']}}
```

#### Native MongoDB Queries

You can also use native MongoDB queries directly:

```java
// Using native MongoDB query syntax
Uni<List<Person>> result = Person.list("{'name': 'John', 'age': {'$gte': 18}}");

// Using Document for complex queries
import org.bson.Document;

Document query = new Document("$and", List.of(
    new Document("age", new Document("$gte", 18)),
    new Document("age", new Document("$lte", 65))
));
Uni<List<Person>> result = Person.list(query);
```

### Pagination

For large datasets, always use pagination:

```java
import io.quarkus.panache.common.Page;

// Create a paginated query
ReactivePanacheQuery<Person> query = Person.find("status", Status.ALIVE);

// Get first page (25 items per page)
Uni<List<Person>> firstPage = query.page(Page.ofSize(25)).list();

// Get next page
Uni<List<Person>> secondPage = query.nextPage().list();

// Get specific page (0-indexed)
Uni<List<Person>> page7 = query.page(Page.of(7, 25)).list();

// Get total count
Uni<Long> totalCount = query.count();

// Get page count
Uni<Integer> pageCount = query.pageCount();
```

### Sorting

```java
import io.quarkus.panache.common.Sort;

// Single field sort
Uni<List<Person>> result = Person.listAll(Sort.by("name"));

// Descending sort
Uni<List<Person>> result = Person.listAll(Sort.by("age").descending());

// Multiple field sort
Uni<List<Person>> result = Person.listAll(Sort.by("lastName").and("firstName"));

// Combined with query
Uni<List<Person>> result = Person.list("status", Sort.by("name").and("age"), Status.ALIVE);
```

---

## Reactive Transactions

MongoDB supports ACID transactions starting from version 4.0 (requires a replica set). Quarkus Reactive MongoDB Panache provides transaction support through the `Panache.withTransaction()` method.

### Basic Transaction Usage

```java
import io.quarkus.mongodb.panache.common.reactive.Panache;

@POST
public Uni<Response> createPersonWithTransaction(Person person) {
    return Panache.withTransaction(() -> 
        person.persist()
            .map(v -> {
                String id = person.id.toString();
                return Response.created(URI.create("/persons/" + id)).build();
            })
    );
}
```

### Transaction with Multiple Operations

```java
public Uni<Void> transferBalance(String fromId, String toId, double amount) {
    return Panache.withTransaction(() -> {
        ObjectId fromOid = new ObjectId(fromId);
        ObjectId toOid = new ObjectId(toId);
        
        return Account.<Account>findById(fromOid)
            .onItem().ifNotNull().transformToUni(fromAccount -> 
                Account.<Account>findById(toOid)
                    .onItem().ifNotNull().transformToUni(toAccount -> {
                        if (fromAccount.balance < amount) {
                            return Uni.createFrom().failure(
                                new IllegalStateException("Insufficient balance"));
                        }
                        
                        fromAccount.balance -= amount;
                        toAccount.balance += amount;
                        
                        return fromAccount.update()
                            .chain(() -> toAccount.update())
                            .replaceWithVoid();
                    })
            );
    });
}
```

### Important Notes on Transactions

1. **Replica Set Required**: MongoDB transactions only work on replica sets. Dev Services automatically configures a single-node replica set.

2. **Session Handling**: Panache handles session management automatically within `withTransaction()`.

3. **Rollback**: If any operation within the transaction fails, all changes are automatically rolled back.

---

## Testing Reactive MongoDB Panache

Testing reactive code requires special consideration due to its asynchronous nature and the requirement to run on a Vert.x event loop.

### Required Test Dependencies

Add the following dependencies to your `pom.xml`:

```xml
<!-- Core testing -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5</artifactId>
    <scope>test</scope>
</dependency>

<!-- For testing reactive code on Vert.x context -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-vertx</artifactId>
    <scope>test</scope>
</dependency>

<!-- For mocking Active Record pattern -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-panache-mock</artifactId>
    <scope>test</scope>
</dependency>

<!-- For mocking Repository pattern -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5-mockito</artifactId>
    <scope>test</scope>
</dependency>

<!-- REST Assured for integration testing -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

**For Gradle:**

```groovy
testImplementation("io.quarkus:quarkus-junit5")
testImplementation("io.quarkus:quarkus-test-vertx")
testImplementation("io.quarkus:quarkus-panache-mock")
testImplementation("io.quarkus:quarkus-junit5-mockito")
testImplementation("io.rest-assured:rest-assured")
```

### Testing Reactive Repository Pattern

The Repository pattern is easier to test because you can use standard Mockito:

```java
package org.acme.repository;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import org.acme.entity.Person;
import org.acme.entity.Status;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class PersonRepositoryMockTest {
    
    @InjectMock
    PersonRepository personRepository;
    
    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {
        // Arrange
        Person person1 = new Person();
        person1.name = "John";
        Person person2 = new Person();
        person2.name = "Jane";
        
        Mockito.when(personRepository.listAll())
               .thenReturn(Uni.createFrom().item(Arrays.asList(person1, person2)));
        
        // Act & Assert
        asserter.assertThat(
            () -> personRepository.listAll(),
            persons -> {
                assertEquals(2, persons.size());
                assertEquals("John", persons.get(0).name);
                assertEquals("Jane", persons.get(1).name);
            }
        );
    }
    
    @Test
    @RunOnVertxContext 
    public void testFindById(UniAsserter asserter) {
        // Arrange
        ObjectId id = new ObjectId();
        Person person = new Person();
        person.id = id;
        person.name = "John";
        
        Mockito.when(personRepository.findById(id))
               .thenReturn(Uni.createFrom().item(person));
        
        // Act & Assert
        asserter.assertThat(
            () -> personRepository.findById(id),
            result -> {
                assertNotNull(result);
                assertEquals("John", result.name);
            }
        );
    }
    
    @Test
    @RunOnVertxContext
    public void testCount(UniAsserter asserter) {
        // Arrange
        Mockito.when(personRepository.count())
               .thenReturn(Uni.createFrom().item(42L));
        
        // Act & Assert
        asserter.assertEquals(() -> personRepository.count(), 42L);
    }
    
    @Test
    @RunOnVertxContext
    public void testPersist(UniAsserter asserter) {
        // Arrange
        Person person = new Person();
        person.name = "New Person";
        
        Mockito.when(personRepository.persist(any(Person.class)))
               .thenReturn(Uni.createFrom().voidItem());
        
        // Act & Assert
        asserter.execute(() -> personRepository.persist(person));
        asserter.execute(() -> {
            Mockito.verify(personRepository).persist(person);
        });
    }
    
    @Test
    @RunOnVertxContext
    public void testDeleteById(UniAsserter asserter) {
        // Arrange
        ObjectId id = new ObjectId();
        
        Mockito.when(personRepository.deleteById(id))
               .thenReturn(Uni.createFrom().item(true));
        
        // Act & Assert
        asserter.assertTrue(() -> personRepository.deleteById(id));
    }
}
```

### Testing Reactive Active Record Pattern

Since Active Record uses static methods, you need the `quarkus-panache-mock` module:

```java
package org.acme.entity;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PersonActiveRecordMockTest {
    
    @Test
    @RunOnVertxContext
    public void testPanacheMocking(UniAsserter asserter) {
        // Setup mock
        asserter.execute(() -> PanacheMock.mock(Person.class));
        
        // Mocked classes always return a default value
        asserter.assertEquals(() -> Person.count(), 0L);
        
        // Specify return value
        asserter.execute(() -> 
            Mockito.when(Person.count())
                   .thenReturn(Uni.createFrom().item(23L)));
        asserter.assertEquals(() -> Person.count(), 23L);
        
        // Change return value
        asserter.execute(() -> 
            Mockito.when(Person.count())
                   .thenReturn(Uni.createFrom().item(42L)));
        asserter.assertEquals(() -> Person.count(), 42L);
        
        // Verify invocations
        asserter.execute(() -> {
            PanacheMock.verify(Person.class, Mockito.times(3)).count();
        });
    }
    
    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {
        // Setup mock
        asserter.execute(() -> PanacheMock.mock(Person.class));
        
        // Arrange
        ObjectId id = new ObjectId();
        Person person = new Person();
        person.id = id;
        person.name = "Test Person";
        
        asserter.execute(() -> 
            Mockito.when(Person.findById(id))
                   .thenReturn(Uni.createFrom().item(person)));
        
        // Act & Assert
        asserter.assertThat(
            () -> Person.findById(id),
            result -> {
                assertNotNull(result);
                assertEquals("Test Person", result.name);
            }
        );
    }
    
    @Test
    @RunOnVertxContext
    public void testListAll(UniAsserter asserter) {
        // Setup mock
        asserter.execute(() -> PanacheMock.mock(Person.class));
        
        // Arrange
        Person person1 = new Person();
        person1.name = "Alice";
        Person person2 = new Person();
        person2.name = "Bob";
        
        asserter.execute(() -> 
            Mockito.when(Person.listAll())
                   .thenReturn(Uni.createFrom().item(Arrays.asList(person1, person2))));
        
        // Act & Assert
        asserter.assertThat(
            () -> Person.listAll(),
            persons -> {
                assertEquals(2, persons.size());
                assertEquals("Alice", persons.get(0).name);
                assertEquals("Bob", persons.get(1).name);
            }
        );
    }
    
    @Test
    @RunOnVertxContext
    public void testCustomStaticMethod(UniAsserter asserter) {
        // Setup mock
        asserter.execute(() -> PanacheMock.mock(Person.class));
        
        // Mock custom static method
        asserter.execute(() -> 
            Mockito.when(Person.findByName("John"))
                   .thenReturn(Uni.createFrom().item(createPerson("John"))));
        
        // Act & Assert
        asserter.assertThat(
            () -> Person.findByName("John"),
            result -> {
                assertNotNull(result);
                assertEquals("John", result.name);
            }
        );
        
        // Verify the custom method was called
        asserter.execute(() -> 
            PanacheMock.verify(Person.class).findByName("John"));
    }
    
    private static Person createPerson(String name) {
        Person person = new Person();
        person.id = new ObjectId();
        person.name = name;
        person.status = Status.ALIVE;
        return person;
    }
}
```

### Integration Testing with Real MongoDB (Dev Services)

For integration tests that use a real MongoDB instance (via Dev Services):

```java
package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.acme.entity.Person;
import org.acme.entity.Status;
import org.acme.repository.PersonRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonRepositoryIntegrationTest {
    
    @Inject
    PersonRepository personRepository;
    
    private static ObjectId createdPersonId;
    
    @Test
    @Order(1)
    @RunOnVertxContext
    public void testCreatePerson(UniAsserter asserter) {
        Person person = new Person();
        person.name = "Integration Test Person";
        person.birthDate = LocalDate.of(1990, 1, 15);
        person.status = Status.ALIVE;
        
        asserter.execute(() -> personRepository.persist(person));
        asserter.execute(() -> {
            assertNotNull(person.id);
            createdPersonId = person.id;
        });
    }
    
    @Test
    @Order(2)
    @RunOnVertxContext
    public void testFindCreatedPerson(UniAsserter asserter) {
        asserter.assertThat(
            () -> personRepository.findById(createdPersonId),
            person -> {
                assertNotNull(person);
                assertEquals("Integration Test Person", person.name);
                assertEquals(Status.ALIVE, person.status);
            }
        );
    }
    
    @Test
    @Order(3)
    @RunOnVertxContext
    public void testUpdatePerson(UniAsserter asserter) {
        asserter.execute(() -> 
            personRepository.findById(createdPersonId)
                .onItem().ifNotNull().transformToUni(person -> {
                    person.status = Status.DECEASED;
                    return personRepository.update(person);
                })
        );
        
        asserter.assertThat(
            () -> personRepository.findById(createdPersonId),
            person -> assertEquals(Status.DECEASED, person.status)
        );
    }
    
    @Test
    @Order(4)
    @RunOnVertxContext
    public void testCountPersons(UniAsserter asserter) {
        asserter.assertThat(
            () -> personRepository.count(),
            count -> assertTrue(count >= 1)
        );
    }
    
    @Test
    @Order(5)
    @RunOnVertxContext
    public void testDeletePerson(UniAsserter asserter) {
        asserter.assertTrue(() -> personRepository.deleteById(createdPersonId));
        asserter.assertNull(() -> personRepository.findById(createdPersonId));
    }
}
```

### REST Integration Testing

```java
package org.acme.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.acme.entity.Person;
import org.acme.entity.Status;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersonResourceIntegrationTest {
    
    private static String createdPersonId;
    
    @Test
    @Order(1)
    public void testCreatePerson() {
        Person person = new Person();
        person.name = "REST Test Person";
        person.birthDate = LocalDate.of(1985, 5, 20);
        person.status = Status.ALIVE;
        
        createdPersonId = given()
            .contentType(ContentType.JSON)
            .body(person)
            .when()
            .post("/persons")
            .then()
            .statusCode(201)
            .header("Location", containsString("/persons/"))
            .extract()
            .path("id.$oid");
    }
    
    @Test
    @Order(2)
    public void testGetPersonById() {
        given()
            .when()
            .get("/persons/" + createdPersonId)
            .then()
            .statusCode(200)
            .body("name", equalTo("REST Test Person"))
            .body("status", equalTo("ALIVE"));
    }
    
    @Test
    @Order(3)
    public void testGetAllPersons() {
        given()
            .when()
            .get("/persons")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }
    
    @Test
    @Order(4)
    public void testUpdatePerson() {
        Person updated = new Person();
        updated.name = "Updated Name";
        updated.status = Status.UNKNOWN;
        
        given()
            .contentType(ContentType.JSON)
            .body(updated)
            .when()
            .put("/persons/" + createdPersonId)
            .then()
            .statusCode(200)
            .body("name", equalTo("Updated Name"))
            .body("status", equalTo("UNKNOWN"));
    }
    
    @Test
    @Order(5)
    public void testGetPersonCount() {
        given()
            .when()
            .get("/persons/count")
            .then()
            .statusCode(200)
            .body(greaterThanOrEqualTo("1"));
    }
    
    @Test
    @Order(6)
    public void testDeletePerson() {
        given()
            .when()
            .delete("/persons/" + createdPersonId)
            .then()
            .statusCode(204);
        
        // Verify deletion
        given()
            .when()
            .get("/persons/" + createdPersonId)
            .then()
            .statusCode(404);
    }
    
    @Test
    public void testGetNonExistentPerson() {
        given()
            .when()
            .get("/persons/000000000000000000000000")  // Non-existent ObjectId
            .then()
            .statusCode(404);
    }
}
```

---

## Best Practices

### 1. Choose the Right Pattern

| Pattern | Use When |
|---------|----------|
| **Active Record** | Simple applications, rapid prototyping, fewer entities |
| **Repository** | Complex applications, better testability, separation of concerns |

### 2. Handle Errors Gracefully

```java
public Uni<Response> getPersonById(String id) {
    return personRepository.findById(new ObjectId(id))
        .onItem().ifNotNull().transform(p -> Response.ok(p).build())
        .onItem().ifNull().continueWith(Response.status(404).build())
        .onFailure(IllegalArgumentException.class)
            .recoverWithItem(Response.status(400).entity("Invalid ID format").build());
}
```

### 3. Use Pagination for Large Datasets

Never use `listAll()` in production for tables with potentially large datasets:

```java
public Uni<List<Person>> getPersonsPaginated(int page, int pageSize) {
    return Person.find("status", Status.ALIVE)
        .page(Page.of(page, pageSize))
        .list();
}
```

### 4. Use Streaming for Very Large Datasets

```java
@GET
@Path("/export")
@Produces(MediaType.SERVER_SENT_EVENTS)
@RestStreamElementType(MediaType.APPLICATION_JSON)
public Multi<Person> exportAllPersons() {
    return Person.streamAll();
}
```

### 5. Enable Query Logging in Development

```properties
%dev.quarkus.log.category."io.quarkus.mongodb.panache.common.reactive.runtime".level=DEBUG
```

### 6. Use Transactions for Data Consistency

```java
public Uni<Void> processOrder(Order order) {
    return Panache.withTransaction(() -> {
        // All operations in this block are transactional
        return order.persist()
            .chain(() -> updateInventory(order))
            .chain(() -> createPayment(order));
    });
}
```

### 7. Use Index Annotations

```java
@MongoEntity(collection = "persons")
public class Person extends ReactivePanacheMongoEntity {
    
    @BsonProperty("email")
    public String email;  // Consider creating an index on frequently queried fields
    
    // Add indexes via MongoDB shell or migration scripts:
    // db.persons.createIndex({ email: 1 }, { unique: true })
}
```

---

## Useful Resources

### Official Quarkus Documentation

- [Quarkus MongoDB with Panache Guide](https://quarkus.io/guides/mongodb-panache) - The official comprehensive guide
- [Quarkus MongoDB Guide](https://quarkus.io/guides/mongodb) - Low-level MongoDB client configuration
- [Mutiny - Reactive Programming Guide](https://quarkus.io/guides/mutiny-primer) - Understanding Mutiny reactive types
- [Quarkus Dev Services](https://quarkus.io/guides/dev-services) - Automatic provisioning of databases for development
- [Testing with Hibernate Reactive Panache](https://quarkus.io/guides/hibernate-reactive-panache#testing) - Testing patterns applicable to MongoDB

### MongoDB Documentation

- [MongoDB Java Driver Documentation](https://www.mongodb.com/docs/drivers/java/sync/current/)
- [MongoDB BSON Types](https://www.mongodb.com/docs/manual/reference/bson-types/)
- [MongoDB Query Operators](https://www.mongodb.com/docs/manual/reference/operator/query/)
- [MongoDB Transactions](https://www.mongodb.com/docs/manual/core/transactions/)

### Quarkus Quickstarts and Examples

- [MongoDB Panache Quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/mongodb-panache-quickstart) - Official example repository

### Smallrye Mutiny Documentation

- [Mutiny Official Documentation](https://smallrye.io/smallrye-mutiny/) - Detailed Mutiny API reference
- [Mutiny Cheatsheet](https://smallrye.io/smallrye-mutiny/2.0.0/guides/cheatsheet/) - Quick reference for common operations

### Additional Learning Resources

- [Baeldung - Quarkus with MongoDB](https://www.baeldung.com/java-quarkus-mongodb) - Tutorial with examples
- [Quarkus YouTube Channel](https://www.youtube.com/@Quarkusio) - Video tutorials and demos
- [Red Hat Developer Blog](https://developers.redhat.com/topics/quarkus) - Articles and best practices

### Community and Support

- [Quarkus GitHub Discussions](https://github.com/quarkusio/quarkus/discussions) - Community Q&A
- [Stack Overflow - Quarkus Tag](https://stackoverflow.com/questions/tagged/quarkus) - Ask questions
- [Quarkus Zulip Chat](https://quarkusio.zulipchat.com/) - Real-time community chat

---

## Summary

This guide covered the essential aspects of building reactive MongoDB applications with Quarkus and Panache:

1. **Reactive Programming Fundamentals**: Understanding Mutiny's `Uni` and `Multi` types
2. **Project Configuration**: Setting up dependencies and MongoDB connection
3. **Two Patterns**: Active Record for simplicity, Repository for separation of concerns
4. **Complete CRUD Operations**: Create, Read, Update, Delete with reactive types
5. **Advanced Querying**: PanacheQL, pagination, sorting, and native queries
6. **Transactions**: ACID transactions with MongoDB replica sets
7. **Comprehensive Testing**: Unit testing with mocks and integration testing

By following this guide and the best practices outlined, you'll be well-equipped to build high-performance, reactive MongoDB applications with Quarkus.

---

*Guide created: January 2025*  
*Quarkus Version: 3.x*  
*MongoDB Driver: Reactive*
