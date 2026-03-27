# Quarkus Reactive Programming Guide with SmallRye Mutiny

> A comprehensive guide for getting started with reactive programming in Quarkus using SmallRye Mutiny and Reactive Panache with MongoDB.

---

## Table of Contents

1. [Introduction to Reactive Programming](#introduction-to-reactive-programming)
2. [SmallRye Mutiny Fundamentals](#smallrye-mutiny-fundamentals)
   - [What is Mutiny?](#what-is-mutiny)
   - [Understanding Uni](#understanding-uni)
   - [Understanding Multi](#understanding-multi)
   - [Uni vs Multi Comparison](#uni-vs-multi-comparison)
3. [Getting Started with Quarkus Reactive](#getting-started-with-quarkus-reactive)
   - [Project Setup](#project-setup)
   - [Dependencies](#dependencies)
4. [Reactive Panache with MongoDB](#reactive-panache-with-mongodb)
   - [Configuration](#configuration)
   - [Creating Reactive Entities](#creating-reactive-entities)
   - [Using Reactive Repositories](#using-reactive-repositories)
   - [CRUD Operations](#crud-operations)
5. [Practical Examples](#practical-examples)
   - [Example 1: Basic Uni Operations](#example-1-basic-uni-operations)
   - [Example 2: Working with Multi](#example-2-working-with-multi)
   - [Example 3: Complete REST API with MongoDB](#example-3-complete-rest-api-with-mongodb)
   - [Example 4: Error Handling](#example-4-error-handling)
   - [Example 5: Combining Uni and Multi](#example-5-combining-uni-and-multi)
6. [Best Practices](#best-practices)
7. [Useful Resources](#useful-resources)

---

## Introduction to Reactive Programming

Reactive programming is a paradigm focused on **asynchronous data streams** and the **propagation of change**. Instead of blocking threads while waiting for I/O operations (like database queries or HTTP requests), reactive applications can handle many concurrent operations efficiently by using non-blocking I/O.

### Why Reactive Programming?

| Traditional (Imperative) | Reactive |
|--------------------------|----------|
| One thread per request | Few threads handle many requests |
| Blocks while waiting for I/O | Non-blocking, event-driven |
| Simple to understand | Requires learning new patterns |
| Limited scalability | Highly scalable |
| Resource-intensive | Resource-efficient |

### Benefits of Reactive in Quarkus

- **Better resource utilization**: Handle more concurrent requests with fewer threads
- **Improved scalability**: Scale efficiently under high load
- **Unified programming model**: Quarkus supports both imperative and reactive styles
- **Cloud-native ready**: Optimized for containerized environments

---

## SmallRye Mutiny Fundamentals

### What is Mutiny?

**SmallRye Mutiny** is an intuitive, event-driven reactive programming library for Java. The name "Mutiny" is actually a contraction of its two core types: **Multi** and **Uni**. Mutiny is the default reactive programming library in Quarkus and provides a developer-friendly API for handling asynchronous operations.

#### Key Characteristics of Mutiny

- **Event-driven**: You react to events (items, failures, completion) as they occur
- **Lazy evaluation**: Nothing happens until you subscribe
- **Navigable API**: The API follows an intuitive `onX().action()` pattern
- **Non-blocking**: Designed for efficient concurrent execution
- **Back-pressure support**: Handles flow control between publishers and subscribers

### Understanding Uni

**`Uni<T>`** represents an **asynchronous action** that will emit either:
- **A single item** of type `T` (can be `null`)
- **A failure** (an exception)

`Uni` is your go-to type when you expect **zero or one result**, such as:
- A single database record lookup
- An HTTP request expecting a single response
- Sending a message to a queue
- Any operation with a single outcome

#### Creating a Uni

```java
import io.smallrye.mutiny.Uni;

// Create a Uni from a known item
Uni<String> uniFromItem = Uni.createFrom().item("Hello, Reactive World!");

// Create a Uni from a null value
Uni<String> uniFromNull = Uni.createFrom().nullItem();

// Create a Uni from a failure
Uni<String> uniFromFailure = Uni.createFrom()
    .failure(new RuntimeException("Something went wrong"));

// Create a Uni from a supplier (lazy evaluation)
Uni<String> uniFromSupplier = Uni.createFrom().item(() -> {
    // This code runs when subscribed
    return computeValue();
});

// Create a Uni from a CompletionStage
Uni<String> uniFromCompletionStage = Uni.createFrom()
    .completionStage(someAsyncOperation());
```

#### Transforming Uni

```java
Uni<String> greeting = Uni.createFrom().item("Hello");

// Transform the item
Uni<String> upperGreeting = greeting
    .onItem().transform(s -> s.toUpperCase());
// Result: "HELLO"

// Chain with another async operation
Uni<Integer> length = greeting
    .onItem().transformToUni(s -> 
        Uni.createFrom().item(s.length())
    );
// Result: 5

// Handle null values
Uni<String> withDefault = greeting
    .onItem().ifNull().continueWith("Default Value");
```

#### Subscribing to Uni

```java
Uni<String> uni = Uni.createFrom().item("Result");

// Subscribe with callbacks
uni.subscribe().with(
    item -> System.out.println("Received: " + item),
    failure -> System.err.println("Failed: " + failure.getMessage())
);

// In Quarkus REST endpoints, you typically just return the Uni
// and Quarkus handles the subscription automatically
@GET
@Path("/{id}")
public Uni<Person> getPerson(@PathParam("id") String id) {
    return Person.findById(id);
}
```

### Understanding Multi

**`Multi<T>`** represents an **asynchronous stream** that emits:
- **Zero or more items** of type `T` (cannot be `null`)
- Optionally followed by a **completion signal** or a **failure**

`Multi` is used when dealing with **streams of data**, such as:
- Multiple database records
- Messages from a message broker topic
- SSE (Server-Sent Events) streams
- Processing collections asynchronously

#### Creating a Multi

```java
import io.smallrye.mutiny.Multi;

// Create a Multi from known items
Multi<String> multiFromItems = Multi.createFrom()
    .items("Apple", "Banana", "Cherry");

// Create a Multi from an Iterable
List<String> fruits = List.of("Apple", "Banana", "Cherry");
Multi<String> multiFromIterable = Multi.createFrom().iterable(fruits);

// Create a Multi from a range
Multi<Integer> range = Multi.createFrom().range(1, 10);
// Emits: 1, 2, 3, 4, 5, 6, 7, 8, 9

// Create an empty Multi
Multi<String> emptyMulti = Multi.createFrom().empty();

// Create a Multi that emits periodically (every 1 second)
Multi<Long> ticks = Multi.createFrom()
    .ticks().every(Duration.ofSeconds(1));
```

#### Transforming Multi

```java
Multi<String> fruits = Multi.createFrom()
    .items("Apple", "Banana", "Cherry", "Date");

// Transform each item
Multi<String> upperFruits = fruits
    .onItem().transform(String::toUpperCase);
// Emits: "APPLE", "BANANA", "CHERRY", "DATE"

// Filter items
Multi<String> filtered = fruits
    .filter(s -> s.startsWith("B"));
// Emits: "Banana"

// Take only first N items
Multi<String> firstTwo = fruits.select().first(2);
// Emits: "Apple", "Banana"

// Skip first N items
Multi<String> skipTwo = fruits.skip().first(2);
// Emits: "Cherry", "Date"

// FlatMap to another Multi
Multi<Character> characters = fruits
    .onItem().transformToMulti(s -> 
        Multi.createFrom().items(s.chars().mapToObj(c -> (char) c)))
    .concatenate();
```

#### Collecting Multi Results

```java
Multi<String> fruits = Multi.createFrom()
    .items("Apple", "Banana", "Cherry");

// Collect to a List (returns Uni<List<T>>)
Uni<List<String>> fruitList = fruits.collect().asList();

// Collect to first item (returns Uni<T>)
Uni<String> firstFruit = fruits.collect().first();

// Collect with a custom collector
Uni<String> joined = fruits.collect()
    .with(Collectors.joining(", "));
// Result: "Apple, Banana, Cherry"
```

### Uni vs Multi Comparison

| Aspect | Uni<T> | Multi<T> |
|--------|--------|----------|
| **Cardinality** | 0 or 1 item | 0 to ∞ items |
| **Null items** | Allowed | Not allowed |
| **Use case** | Single async result | Streams of data |
| **Completion** | After item/failure | Explicit completion signal |
| **Back-pressure** | Not applicable | Supported |
| **Examples** | DB find by ID, HTTP call | DB list all, message streams |

---

## Getting Started with Quarkus Reactive

### Project Setup

Create a new Quarkus project with reactive MongoDB support using the Quarkus CLI:

```bash
# Create a new project with reactive MongoDB Panache
quarkus create app com.example:reactive-mongodb-demo \
    --extension=resteasy-reactive-jackson,mongodb-panache \
    --no-code

cd reactive-mongodb-demo
```

Or using Maven:

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.0:create \
    -DprojectGroupId=com.example \
    -DprojectArtifactId=reactive-mongodb-demo \
    -Dextensions="resteasy-reactive-jackson,mongodb-panache" \
    -DnoCode
```

### Dependencies

Your `pom.xml` should include these essential dependencies:

```xml
<dependencies>
    <!-- RESTEasy Reactive with Jackson for JSON -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
    </dependency>
    
    <!-- MongoDB Panache (includes reactive support) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-mongodb-panache</artifactId>
    </dependency>
    
    <!-- SmallRye Mutiny (usually included transitively) -->
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny</artifactId>
    </dependency>
    
    <!-- Test dependencies -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-junit5</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Reactive Panache with MongoDB

### Configuration

Configure MongoDB connection in `src/main/resources/application.properties`:

```properties
# MongoDB connection configuration
quarkus.mongodb.connection-string=mongodb://localhost:27017
quarkus.mongodb.database=mydatabase

# Optional: Configure connection pool
quarkus.mongodb.max-pool-size=50
quarkus.mongodb.min-pool-size=5

# Optional: Configure timeouts
quarkus.mongodb.connect-timeout=10s
quarkus.mongodb.read-timeout=30s

# Dev Services - automatically starts MongoDB container in dev mode
quarkus.mongodb.devservices.enabled=true
```

### Creating Reactive Entities

Quarkus Panache provides two approaches for reactive MongoDB entities:

#### Approach 1: Active Record Pattern (Extending ReactivePanacheMongoEntity)

```java
package com.example.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.types.ObjectId;

import java.time.LocalDate;

@MongoEntity(collection = "persons")
public class Person extends ReactivePanacheMongoEntity {
    // id field is inherited from ReactivePanacheMongoEntity (ObjectId type)
    
    public String name;
    public String email;
    public LocalDate birthDate;
    public Status status;
    
    // Default constructor required
    public Person() {}
    
    public Person(String name, String email, LocalDate birthDate) {
        this.name = name;
        this.email = email;
        this.birthDate = birthDate;
        this.status = Status.ACTIVE;
    }
    
    public enum Status {
        ACTIVE, INACTIVE
    }
}
```

#### Approach 2: Custom ID with ReactivePanacheMongoEntityBase

```java
package com.example.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import org.bson.codecs.pojo.annotations.BsonId;

@MongoEntity(collection = "products")
public class Product extends ReactivePanacheMongoEntityBase {
    
    @BsonId
    public String sku;  // Custom String ID instead of ObjectId
    
    public String name;
    public String description;
    public double price;
    public int quantity;
    
    public Product() {}
    
    public Product(String sku, String name, double price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = 0;
    }
}
```

### Using Reactive Repositories

The Repository pattern provides a cleaner separation of concerns:

```java
package com.example.repository;

import com.example.entity.Person;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PersonRepository implements ReactivePanacheMongoRepository<Person> {
    
    // Custom query methods
    public Uni<Person> findByEmail(String email) {
        return find("email", email).firstResult();
    }
    
    public Multi<Person> findByStatus(Person.Status status) {
        return find("status", status).stream();
    }
    
    public Multi<Person> findByNameContaining(String namePart) {
        return find("name like ?1", ".*" + namePart + ".*").stream();
    }
    
    public Uni<Long> countByStatus(Person.Status status) {
        return count("status", status);
    }
    
    public Uni<Long> deleteInactive() {
        return delete("status", Person.Status.INACTIVE);
    }
}
```

### CRUD Operations

#### Create (Persist)

```java
// Using Active Record pattern
Person person = new Person("John Doe", "john@example.com", LocalDate.of(1990, 5, 15));
Uni<Person> savedPerson = person.persist();

// The ID is populated after persist completes
savedPerson.subscribe().with(
    p -> System.out.println("Saved with ID: " + p.id)
);

// Using Repository pattern
@Inject
PersonRepository personRepository;

Uni<Person> saved = personRepository.persist(person);
```

#### Read (Find)

```java
// Find by ID
Uni<Person> personById = Person.findById(new ObjectId("507f1f77bcf86cd799439011"));

// Find by ID with Optional
Uni<Optional<Person>> optionalPerson = Person.findByIdOptional(objectId);

// Find all
Uni<List<Person>> allPersons = Person.listAll();

// Find with query
Uni<List<Person>> activePersons = Person.list("status", Person.Status.ACTIVE);

// Stream results (returns Multi)
Multi<Person> personStream = Person.streamAll();

// Find with complex query
Uni<List<Person>> filtered = Person.list(
    "status = ?1 and birthDate > ?2", 
    Person.Status.ACTIVE, 
    LocalDate.of(2000, 1, 1)
);

// Find with document query
Uni<List<Person>> advancedQuery = Person.list(
    Document.parse("{ 'status': 'ACTIVE', 'name': { '$regex': '.*John.*' } }")
);
```

#### Update

```java
// Update existing entity
Uni<Person> updatePerson(ObjectId id, String newEmail) {
    return Person.<Person>findById(id)
        .onItem().ifNotNull().transform(person -> {
            person.email = newEmail;
            return person;
        })
        .onItem().ifNotNull().transformToUni(person -> person.update());
}

// Bulk update
Uni<Long> deactivateOldPersons = Person.update("status", Person.Status.INACTIVE)
    .where("birthDate < ?1", LocalDate.of(1950, 1, 1));
```

#### Delete

```java
// Delete entity
Uni<Void> deletePerson = person.delete();

// Delete by ID
Uni<Boolean> deleted = Person.deleteById(objectId);

// Delete by query
Uni<Long> deleteCount = Person.delete("status", Person.Status.INACTIVE);

// Delete all
Uni<Long> deleteAllCount = Person.deleteAll();
```

---

## Practical Examples

### Example 1: Basic Uni Operations

This example demonstrates fundamental Uni operations and transformations:

```java
package com.example;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;

@ApplicationScoped
public class UniExamples {
    
    // Step 1: Create a simple Uni
    public Uni<String> getGreeting(String name) {
        return Uni.createFrom().item("Hello, " + name + "!");
    }
    
    // Step 2: Transform the result
    public Uni<String> getUppercaseGreeting(String name) {
        return getGreeting(name)
            .onItem().transform(String::toUpperCase);
    }
    
    // Step 3: Chain multiple async operations
    public Uni<UserInfo> getUserInfo(String userId) {
        return fetchUser(userId)
            .onItem().transformToUni(user -> 
                fetchUserPreferences(user.id)
                    .onItem().transform(prefs -> 
                        new UserInfo(user, prefs))
            );
    }
    
    // Step 4: Handle potential null values
    public Uni<String> findUserNameOrDefault(String userId) {
        return fetchUser(userId)
            .onItem().ifNotNull().transform(user -> user.name)
            .onItem().ifNull().continueWith("Anonymous");
    }
    
    // Step 5: Add timeout
    public Uni<String> getDataWithTimeout(String key) {
        return fetchData(key)
            .ifNoItem().after(Duration.ofSeconds(5))
            .fail();
    }
    
    // Step 6: Retry on failure
    public Uni<String> fetchWithRetry(String url) {
        return callExternalService(url)
            .onFailure().retry()
            .withBackOff(Duration.ofMillis(100), Duration.ofSeconds(1))
            .atMost(3);
    }
    
    // Helper methods (simulated async operations)
    private Uni<User> fetchUser(String id) {
        return Uni.createFrom().item(new User(id, "John Doe"));
    }
    
    private Uni<UserPreferences> fetchUserPreferences(String userId) {
        return Uni.createFrom().item(new UserPreferences(userId, "dark", "en"));
    }
    
    private Uni<String> fetchData(String key) {
        return Uni.createFrom().item("data-" + key);
    }
    
    private Uni<String> callExternalService(String url) {
        return Uni.createFrom().item("response from " + url);
    }
    
    // Inner classes for demonstration
    record User(String id, String name) {}
    record UserPreferences(String userId, String theme, String language) {}
    record UserInfo(User user, UserPreferences preferences) {}
}
```

### Example 2: Working with Multi

This example shows how to work with streams using Multi:

```java
package com.example;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class MultiExamples {
    
    // Step 1: Create a Multi from items
    public Multi<String> getFruits() {
        return Multi.createFrom()
            .items("Apple", "Banana", "Cherry", "Date", "Elderberry");
    }
    
    // Step 2: Filter items
    public Multi<String> getFruitsStartingWith(String prefix) {
        return getFruits()
            .filter(fruit -> fruit.startsWith(prefix));
    }
    
    // Step 3: Transform each item
    public Multi<FruitInfo> getFruitInfos() {
        return getFruits()
            .onItem().transform(name -> new FruitInfo(name, name.length()));
    }
    
    // Step 4: Limit and skip items
    public Multi<String> getPagedFruits(int page, int size) {
        return getFruits()
            .skip().first((long) page * size)
            .select().first(size);
    }
    
    // Step 5: Collect to various types
    public Uni<List<String>> getAllFruitsAsList() {
        return getFruits().collect().asList();
    }
    
    public Uni<String> getAllFruitsJoined() {
        return getFruits().collect().with(Collectors.joining(", "));
    }
    
    // Step 6: Process items in batches
    public Multi<List<String>> getFruitsInBatches(int batchSize) {
        return getFruits()
            .group().intoLists().of(batchSize);
    }
    
    // Step 7: Merge multiple Multi streams
    public Multi<String> getAllProducts() {
        Multi<String> fruits = getFruits();
        Multi<String> vegetables = Multi.createFrom()
            .items("Carrot", "Broccoli", "Spinach");
        
        return Multi.createBy().merging().streams(fruits, vegetables);
    }
    
    // Step 8: Rate limiting with ticks
    public Multi<String> getFruitsWithDelay() {
        AtomicInteger index = new AtomicInteger(0);
        List<String> fruitList = List.of("Apple", "Banana", "Cherry");
        
        return Multi.createFrom()
            .ticks().every(Duration.ofSeconds(1))
            .select().first(fruitList.size())
            .onItem().transform(tick -> fruitList.get(index.getAndIncrement()));
    }
    
    // Step 9: FlatMap for async processing of each item
    public Multi<EnrichedFruit> getEnrichedFruits() {
        return getFruits()
            .onItem().transformToUniAndMerge(name -> 
                fetchFruitDetails(name)
                    .onItem().transform(details -> 
                        new EnrichedFruit(name, details)));
    }
    
    // Step 10: Handle completion and count
    public Uni<Long> countFruits() {
        return getFruits()
            .collect().with(Collectors.counting());
    }
    
    // Helper method
    private Uni<FruitDetails> fetchFruitDetails(String name) {
        return Uni.createFrom().item(new FruitDetails(
            name.hashCode() % 100 + " kcal", 
            name.length() > 5 ? "Tropical" : "Common"
        ));
    }
    
    // Inner classes
    record FruitInfo(String name, int nameLength) {}
    record FruitDetails(String calories, String category) {}
    record EnrichedFruit(String name, FruitDetails details) {}
}
```

### Example 3: Complete REST API with MongoDB

A complete example showing a reactive REST API with MongoDB Panache:

**Step 1: Create the Entity**

```java
package com.example.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@MongoEntity(collection = "tasks")
public class Task extends ReactivePanacheMongoEntity {
    
    public String title;
    public String description;
    public TaskStatus status;
    public Priority priority;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime dueDate;
    
    public Task() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
    }
    
    public Task(String title, String description, Priority priority) {
        this();
        this.title = title;
        this.description = description;
        this.priority = priority;
    }
    
    // Custom query methods
    public static Uni<List<Task>> findByStatus(TaskStatus status) {
        return list("status", status);
    }
    
    public static Multi<Task> streamByStatus(TaskStatus status) {
        return stream("status", status);
    }
    
    public static Uni<List<Task>> findByPriority(Priority priority) {
        return list("priority", priority);
    }
    
    public static Uni<List<Task>> findPending() {
        return list("status", TaskStatus.PENDING);
    }
    
    public static Uni<Long> countByStatus(TaskStatus status) {
        return count("status", status);
    }
    
    public static Uni<Long> deleteCompleted() {
        return delete("status", TaskStatus.COMPLETED);
    }
    
    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
    
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
```

**Step 2: Create the Service**

```java
package com.example.service;

import com.example.entity.Task;
import com.example.entity.Task.TaskStatus;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TaskService {
    
    public Multi<Task> getAllTasks() {
        return Task.streamAll();
    }
    
    public Uni<List<Task>> getTaskList() {
        return Task.listAll();
    }
    
    public Uni<Task> getTaskById(String id) {
        return Task.<Task>findById(new ObjectId(id))
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Task not found: " + id));
    }
    
    public Uni<Task> createTask(Task task) {
        task.createdAt = LocalDateTime.now();
        task.updatedAt = LocalDateTime.now();
        return task.persist();
    }
    
    public Uni<Task> updateTask(String id, Task updatedTask) {
        return getTaskById(id)
            .onItem().transform(existing -> {
                existing.title = updatedTask.title;
                existing.description = updatedTask.description;
                existing.priority = updatedTask.priority;
                existing.status = updatedTask.status;
                existing.dueDate = updatedTask.dueDate;
                existing.updatedAt = LocalDateTime.now();
                return existing;
            })
            .onItem().transformToUni(Task::update);
    }
    
    public Uni<Task> updateTaskStatus(String id, TaskStatus status) {
        return getTaskById(id)
            .onItem().transform(task -> {
                task.status = status;
                task.updatedAt = LocalDateTime.now();
                return task;
            })
            .onItem().transformToUni(Task::update);
    }
    
    public Uni<Boolean> deleteTask(String id) {
        return Task.deleteById(new ObjectId(id));
    }
    
    public Multi<Task> getTasksByStatus(TaskStatus status) {
        return Task.streamByStatus(status);
    }
    
    public Uni<TaskStats> getTaskStats() {
        return Uni.combine().all().unis(
            Task.count(),
            Task.countByStatus(TaskStatus.PENDING),
            Task.countByStatus(TaskStatus.IN_PROGRESS),
            Task.countByStatus(TaskStatus.COMPLETED)
        ).asTuple().onItem().transform(tuple -> 
            new TaskStats(
                tuple.getItem1(),
                tuple.getItem2(),
                tuple.getItem3(),
                tuple.getItem4()
            )
        );
    }
    
    public record TaskStats(
        long total, 
        long pending, 
        long inProgress, 
        long completed
    ) {}
}
```

**Step 3: Create the REST Resource**

```java
package com.example.resource;

import com.example.entity.Task;
import com.example.entity.Task.TaskStatus;
import com.example.service.TaskService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.net.URI;
import java.util.List;

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {
    
    @Inject
    TaskService taskService;
    
    @GET
    public Uni<List<Task>> getAllTasks() {
        return taskService.getTaskList();
    }
    
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Task> streamAllTasks() {
        return taskService.getAllTasks();
    }
    
    @GET
    @Path("/{id}")
    public Uni<Task> getTask(@PathParam("id") String id) {
        return taskService.getTaskById(id);
    }
    
    @POST
    public Uni<Response> createTask(Task task) {
        return taskService.createTask(task)
            .onItem().transform(created -> 
                Response.created(URI.create("/api/tasks/" + created.id))
                    .entity(created)
                    .build());
    }
    
    @PUT
    @Path("/{id}")
    public Uni<Task> updateTask(@PathParam("id") String id, Task task) {
        return taskService.updateTask(id, task);
    }
    
    @PATCH
    @Path("/{id}/status")
    public Uni<Task> updateTaskStatus(
            @PathParam("id") String id, 
            @QueryParam("status") TaskStatus status) {
        return taskService.updateTaskStatus(id, status);
    }
    
    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteTask(@PathParam("id") String id) {
        return taskService.deleteTask(id)
            .onItem().transform(deleted -> deleted 
                ? Response.noContent().build()
                : Response.status(Status.NOT_FOUND).build());
    }
    
    @GET
    @Path("/status/{status}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Task> getTasksByStatus(@PathParam("status") TaskStatus status) {
        return taskService.getTasksByStatus(status);
    }
    
    @GET
    @Path("/stats")
    public Uni<TaskService.TaskStats> getStats() {
        return taskService.getTaskStats();
    }
}
```

### Example 4: Error Handling

Comprehensive error handling patterns with Mutiny:

```java
package com.example;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Duration;

@ApplicationScoped
public class ErrorHandlingExamples {
    
    // Pattern 1: Recover with a default value on any failure
    public Uni<String> getConfigWithDefault(String key) {
        return fetchConfig(key)
            .onFailure().recoverWithItem("default-value");
    }
    
    // Pattern 2: Recover with a computed value
    public Uni<String> getConfigWithFallback(String key) {
        return fetchConfig(key)
            .onFailure().recoverWithUni(failure -> 
                fetchFromBackupSource(key));
    }
    
    // Pattern 3: Transform specific exceptions
    public Uni<User> findUserOrThrow(String id) {
        return fetchUser(id)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("User not found: " + id))
            .onFailure(DatabaseException.class).transform(e -> 
                new WebApplicationException("Database error", 
                    Response.Status.SERVICE_UNAVAILABLE));
    }
    
    // Pattern 4: Retry with exponential backoff
    public Uni<String> fetchWithRetry(String url) {
        return callExternalApi(url)
            .onFailure().retry()
            .withBackOff(Duration.ofMillis(100), Duration.ofSeconds(2))
            .withJitter(0.2)
            .atMost(5)
            .onFailure().recoverWithItem("fallback-response");
    }
    
    // Pattern 5: Retry only on specific exceptions
    public Uni<String> fetchWithSelectiveRetry(String url) {
        return callExternalApi(url)
            .onFailure(TransientException.class).retry()
            .atMost(3)
            .onFailure(PermanentException.class).recoverWithItem("error");
    }
    
    // Pattern 6: Add timeout with failure handling
    public Uni<String> fetchWithTimeout(String url) {
        return callExternalApi(url)
            .ifNoItem().after(Duration.ofSeconds(5))
            .failWith(new TimeoutException("Request timed out"))
            .onFailure(TimeoutException.class).recoverWithItem("timeout-fallback");
    }
    
    // Pattern 7: Log failures without stopping the pipeline
    public Uni<String> fetchWithLogging(String url) {
        return callExternalApi(url)
            .onFailure().invoke(failure -> 
                System.err.println("Request failed: " + failure.getMessage()))
            .onFailure().recoverWithItem("logged-fallback");
    }
    
    // Pattern 8: Circuit breaker pattern (basic)
    private volatile int failureCount = 0;
    private volatile long lastFailureTime = 0;
    private static final int FAILURE_THRESHOLD = 5;
    private static final long RESET_TIMEOUT_MS = 30000;
    
    public Uni<String> fetchWithCircuitBreaker(String url) {
        if (isCircuitOpen()) {
            return Uni.createFrom().item("circuit-open-fallback");
        }
        
        return callExternalApi(url)
            .onItem().invoke(item -> resetCircuit())
            .onFailure().invoke(failure -> recordFailure())
            .onFailure().recoverWithItem("fallback");
    }
    
    private boolean isCircuitOpen() {
        if (failureCount >= FAILURE_THRESHOLD) {
            if (System.currentTimeMillis() - lastFailureTime > RESET_TIMEOUT_MS) {
                resetCircuit();
                return false;
            }
            return true;
        }
        return false;
    }
    
    private void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
    }
    
    private void resetCircuit() {
        failureCount = 0;
    }
    
    // Helper methods and exceptions
    private Uni<String> fetchConfig(String key) {
        return Uni.createFrom().item("config-" + key);
    }
    
    private Uni<String> fetchFromBackupSource(String key) {
        return Uni.createFrom().item("backup-" + key);
    }
    
    private Uni<User> fetchUser(String id) {
        return Uni.createFrom().item(new User(id, "John"));
    }
    
    private Uni<String> callExternalApi(String url) {
        return Uni.createFrom().item("response from " + url);
    }
    
    record User(String id, String name) {}
    
    static class DatabaseException extends RuntimeException {
        public DatabaseException(String message) { super(message); }
    }
    
    static class TransientException extends RuntimeException {
        public TransientException(String message) { super(message); }
    }
    
    static class PermanentException extends RuntimeException {
        public PermanentException(String message) { super(message); }
    }
    
    static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) { super(message); }
    }
}
```

### Example 5: Combining Uni and Multi

Advanced patterns for combining multiple reactive streams:

```java
package com.example;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CombiningExamples {
    
    // Pattern 1: Combine multiple Unis into a tuple
    public Uni<UserProfile> getUserProfile(String userId) {
        return Uni.combine().all().unis(
            fetchUser(userId),
            fetchUserPreferences(userId),
            fetchUserStats(userId)
        ).asTuple().onItem().transform(tuple -> 
            new UserProfile(
                tuple.getItem1(),
                tuple.getItem2(),
                tuple.getItem3()
            )
        );
    }
    
    // Pattern 2: Combine with custom combinator
    public Uni<OrderSummary> getOrderSummary(String orderId) {
        return Uni.combine().all().unis(
            fetchOrder(orderId),
            fetchOrderItems(orderId),
            fetchShippingInfo(orderId)
        ).combinedWith((order, items, shipping) -> 
            new OrderSummary(order, items, shipping)
        );
    }
    
    // Pattern 3: Sequential execution (when order matters)
    public Uni<String> processInSequence(String data) {
        return validate(data)
            .onItem().transformToUni(validated -> 
                transform(validated))
            .onItem().transformToUni(transformed -> 
                persist(transformed))
            .onItem().transformToUni(persisted -> 
                notify(persisted));
    }
    
    // Pattern 4: Parallel execution and combine results
    public Uni<DashboardData> getDashboard(String userId) {
        Uni<List<Task>> tasks = fetchUserTasks(userId);
        Uni<List<Notification>> notifications = fetchNotifications(userId);
        Uni<UserStats> stats = fetchUserStats(userId);
        
        return Uni.combine().all()
            .unis(tasks, notifications, stats)
            .combinedWith(DashboardData::new);
    }
    
    // Pattern 5: Zip Multis together
    public Multi<Tuple2<String, Integer>> zipStreams() {
        Multi<String> names = Multi.createFrom()
            .items("Alice", "Bob", "Charlie");
        Multi<Integer> scores = Multi.createFrom()
            .items(95, 87, 92);
        
        return Multi.createBy().combining()
            .streams(names, scores)
            .asTuple();
    }
    
    // Pattern 6: Merge multiple Multis
    public Multi<Event> getAllEvents() {
        Multi<Event> userEvents = fetchUserEvents();
        Multi<Event> systemEvents = fetchSystemEvents();
        Multi<Event> externalEvents = fetchExternalEvents();
        
        return Multi.createBy().merging()
            .streams(userEvents, systemEvents, externalEvents);
    }
    
    // Pattern 7: Concatenate Multis (preserve order)
    public Multi<String> getOrderedData() {
        Multi<String> priority = Multi.createFrom().items("URGENT: Alert!");
        Multi<String> normal = Multi.createFrom().items("Info 1", "Info 2");
        Multi<String> low = Multi.createFrom().items("Debug: trace");
        
        return Multi.createBy().concatenating()
            .streams(priority, normal, low);
    }
    
    // Pattern 8: Convert between Uni and Multi
    public Uni<List<String>> multiToUni() {
        return Multi.createFrom()
            .items("A", "B", "C")
            .collect().asList();
    }
    
    public Multi<String> uniToMulti() {
        return Uni.createFrom()
            .item(List.of("A", "B", "C"))
            .onItem().transformToMulti(list -> 
                Multi.createFrom().iterable(list));
    }
    
    // Pattern 9: Fan-out (process each item in a list)
    public Uni<List<ProcessedItem>> fanOutProcess(List<String> ids) {
        return Multi.createFrom().iterable(ids)
            .onItem().transformToUniAndMerge(id -> processItem(id))
            .collect().asList();
    }
    
    // Pattern 10: First successful result
    public Uni<String> getFromFastestSource(String key) {
        return Uni.combine().any().of(
            fetchFromSource1(key),
            fetchFromSource2(key),
            fetchFromSource3(key)
        );
    }
    
    // Helper methods and records
    private Uni<User> fetchUser(String id) {
        return Uni.createFrom().item(new User(id, "John"));
    }
    
    private Uni<Preferences> fetchUserPreferences(String userId) {
        return Uni.createFrom().item(new Preferences("dark", "en"));
    }
    
    private Uni<UserStats> fetchUserStats(String userId) {
        return Uni.createFrom().item(new UserStats(100, 50));
    }
    
    private Uni<Order> fetchOrder(String id) {
        return Uni.createFrom().item(new Order(id, "PENDING"));
    }
    
    private Uni<List<OrderItem>> fetchOrderItems(String orderId) {
        return Uni.createFrom().item(List.of(new OrderItem("item1", 2)));
    }
    
    private Uni<ShippingInfo> fetchShippingInfo(String orderId) {
        return Uni.createFrom().item(new ShippingInfo("Express", "NYC"));
    }
    
    private Uni<List<Task>> fetchUserTasks(String userId) {
        return Uni.createFrom().item(List.of());
    }
    
    private Uni<List<Notification>> fetchNotifications(String userId) {
        return Uni.createFrom().item(List.of());
    }
    
    private Multi<Event> fetchUserEvents() {
        return Multi.createFrom().items(new Event("user", "login"));
    }
    
    private Multi<Event> fetchSystemEvents() {
        return Multi.createFrom().items(new Event("system", "startup"));
    }
    
    private Multi<Event> fetchExternalEvents() {
        return Multi.createFrom().items(new Event("external", "webhook"));
    }
    
    private Uni<String> validate(String data) {
        return Uni.createFrom().item(data);
    }
    
    private Uni<String> transform(String data) {
        return Uni.createFrom().item(data.toUpperCase());
    }
    
    private Uni<String> persist(String data) {
        return Uni.createFrom().item("persisted:" + data);
    }
    
    private Uni<String> notify(String data) {
        return Uni.createFrom().item("notified:" + data);
    }
    
    private Uni<ProcessedItem> processItem(String id) {
        return Uni.createFrom().item(new ProcessedItem(id, "processed"));
    }
    
    private Uni<String> fetchFromSource1(String key) {
        return Uni.createFrom().item("source1-" + key);
    }
    
    private Uni<String> fetchFromSource2(String key) {
        return Uni.createFrom().item("source2-" + key);
    }
    
    private Uni<String> fetchFromSource3(String key) {
        return Uni.createFrom().item("source3-" + key);
    }
    
    // Record definitions
    record User(String id, String name) {}
    record Preferences(String theme, String language) {}
    record UserStats(int posts, int followers) {}
    record UserProfile(User user, Preferences preferences, UserStats stats) {}
    record Order(String id, String status) {}
    record OrderItem(String productId, int quantity) {}
    record ShippingInfo(String method, String destination) {}
    record OrderSummary(Order order, List<OrderItem> items, ShippingInfo shipping) {}
    record Task(String id, String title) {}
    record Notification(String id, String message) {}
    record DashboardData(List<Task> tasks, List<Notification> notifications, UserStats stats) {}
    record Event(String type, String action) {}
    record ProcessedItem(String id, String result) {}
}
```

---

## Best Practices

### 1. Always Return Reactive Types from Public Methods

```java
// ✅ Good - Returns Uni, letting caller decide when to subscribe
public Uni<User> findUser(String id) {
    return User.findById(id);
}

// ❌ Bad - Blocks the caller
public User findUserBlocking(String id) {
    return User.<User>findById(id).await().indefinitely();
}
```

### 2. Use Appropriate Error Handling

```java
// ✅ Good - Specific error handling
public Uni<User> getUser(String id) {
    return User.<User>findById(id)
        .onItem().ifNull().failWith(() -> new NotFoundException("User not found"))
        .onFailure(MongoException.class).transform(e -> 
            new ServiceUnavailableException("Database unavailable"));
}
```

### 3. Avoid Blocking Operations

```java
// ❌ Bad - Mixing blocking with reactive
public Uni<String> processData(String input) {
    // This blocks the reactive thread!
    String result = expensiveBlockingOperation(input);
    return Uni.createFrom().item(result);
}

// ✅ Good - Run blocking operation on worker thread
public Uni<String> processDataCorrectly(String input) {
    return Uni.createFrom()
        .item(() -> expensiveBlockingOperation(input))
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
}
```

### 4. Use Multi for Streaming Large Datasets

```java
// ✅ Good - Streams data efficiently
@GET
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<User> streamAllUsers() {
    return User.streamAll();
}

// ⚠️ Consider carefully - Loads all data into memory
@GET
public Uni<List<User>> getAllUsers() {
    return User.listAll();
}
```

### 5. Combine Operations Efficiently

```java
// ✅ Good - Parallel execution
public Uni<DashboardData> getDashboard(String userId) {
    return Uni.combine().all().unis(
        fetchProfile(userId),
        fetchStats(userId),
        fetchNotifications(userId)
    ).combinedWith(DashboardData::new);
}

// ❌ Bad - Sequential execution (unnecessary)
public Uni<DashboardData> getDashboardSlow(String userId) {
    return fetchProfile(userId)
        .onItem().transformToUni(profile ->
            fetchStats(userId).onItem().transformToUni(stats ->
                fetchNotifications(userId).onItem().transform(notifications ->
                    new DashboardData(profile, stats, notifications))));
}
```

### 6. Handle Null Values Explicitly

```java
// ✅ Good - Explicit null handling
public Uni<Response> getUser(String id) {
    return User.<User>findById(id)
        .onItem().ifNotNull().transform(user -> 
            Response.ok(user).build())
        .onItem().ifNull().continueWith(() -> 
            Response.status(Status.NOT_FOUND).build());
}
```

---

## Useful Resources

### Official Documentation

- **[SmallRye Mutiny Documentation](https://smallrye.io/smallrye-mutiny/latest/)** - Complete guide to Mutiny's API and patterns
- **[Quarkus Mutiny Guide](https://quarkus.io/guides/mutiny-primer)** - Quarkus-specific introduction to Mutiny
- **[Quarkus MongoDB Panache Guide](https://quarkus.io/guides/mongodb-panache)** - Official guide for MongoDB with Panache
- **[Quarkus Reactive Guide](https://quarkus.io/guides/getting-started-reactive)** - Getting started with reactive Quarkus

### Tutorials and Guides

- **[Mutiny Tutorials](https://smallrye.io/smallrye-mutiny/latest/tutorials/hello-mutiny/)** - Step-by-step Mutiny tutorials
- **[Quarkus Reactive REST Guide](https://quarkus.io/guides/resteasy-reactive)** - Building reactive REST services
- **[Reactive Messaging with Quarkus](https://quarkus.io/guides/kafka-reactive-getting-started)** - Event-driven architecture

### API References

- **[Mutiny API Javadoc](https://smallrye.io/smallrye-mutiny/latest/apidocs/)** - Complete API reference
- **[Panache Reactive API](https://quarkus.io/guides/mongodb-panache#reactive)** - Reactive Panache reference

### Community Resources

- **[Quarkus GitHub Repository](https://github.com/quarkusio/quarkus)** - Source code and examples
- **[Quarkus Zulip Chat](https://quarkusio.zulipchat.com/)** - Community discussions
- **[Stack Overflow - Quarkus Tag](https://stackoverflow.com/questions/tagged/quarkus)** - Q&A community
- **[Quarkus Blog](https://quarkus.io/blog/)** - Latest news and tutorials

### Books and Courses

- **[Reactive Systems in Java](https://www.oreilly.com/library/view/reactive-systems-in/9781492091714/)** - O'Reilly book covering reactive patterns
- **[Quarkus for Spring Developers](https://developers.redhat.com/e-books/quarkus-spring-developers)** - Red Hat e-book for Spring developers

### Video Tutorials

- **[Quarkus YouTube Channel](https://www.youtube.com/c/Quarkusio)** - Official video tutorials
- **[DevNation Tech Talks](https://developers.redhat.com/devnation)** - Red Hat developer sessions

---

## Summary

This guide covered the fundamentals of reactive programming in Quarkus using SmallRye Mutiny:

1. **Uni** - For single async results (0 or 1 item)
2. **Multi** - For streams of data (0 to ∞ items)
3. **Reactive Panache** - Simplified MongoDB operations with reactive types
4. **Error Handling** - Patterns for robust failure management
5. **Combining Streams** - Techniques for complex data orchestration

Reactive programming with Quarkus enables you to build highly scalable, resource-efficient applications. Start with simple Uni operations, gradually incorporate Multi for streaming scenarios, and always prioritize non-blocking patterns for optimal performance.

---

*Last updated: January 2026*
*Quarkus version: 3.x*
*SmallRye Mutiny version: 2.x*
