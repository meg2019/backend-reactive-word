# Backend Reactive Word Service

A reactive gRPC server built with Quarkus for vocabulary and translation management. This service provides word pair translations organized by topics, backed by MongoDB with reactive streaming support.

## Overview

This service stores vocabulary concepts with multilingual word translations. Each concept belongs to a topic (category) and contains words in different languages with metadata (part of speech, audio pronunciation URLs, comments).

## gRPC Endpoints

The WordService provides the following RPC methods:

| Method | Request | Response | Description |
|--------|---------|----------|-------------|
| getTopicCount | Empty | TopicCount | Returns the total number of unique vocabulary topics |
| getTopics | Empty | stream Topic | Streams all available topic names and descriptions |
| getWordPairs | WordPairRequest | stream WordPair | Streams word translation pairs for a specific topic and language pair |

### Message Types

- **Topic**: Contains `name` and `description` fields
- **WordPair**: Contains `source_word` and `target_word` fields
- **WordPairRequest**: Specifies `topic_name`, `source_language`, and `target_language` (ISO 639-1 codes, e.g., "en", "he", "ru")

## Configuration

### Network

| Environment | Address | Port |
|-------------|---------|------|
| Development | `localhost` | `9000` |
| Production | `0.0.0.0` | `9000` |
| Test | `localhost` | `9001` |

gRPC reflection is enabled for development/testing with tools like `grpcurl`.

### Database

- **MongoDB Database**: `words`
- **Connection (prod)**: `mongodb://mongo:27017`
- **Collection**: `concepts`
- **Migrations**: Liquibase MongoDB with changelog at `db/changelog/db.changelog-master.yaml`

## Running the Application

### Development Mode (with auto-reload)

```shell
./mvnw quarkus:dev
```

MongoDB Dev Services will automatically start a MongoDB instance.

### Production Build

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Executable

```shell
./mvnw package -Dnative
./target/backend-reactive-word-1.0.0-SNAPSHOT-runner
```

## Testing with grpcurl

```shell
# Get topic count
grpcurl -plaintext localhost:9000 backend.WordService/getTopicCount

# List all topics
grpcurl -plaintext localhost:9000 backend.WordService/getTopics

# Get word pairs for a topic
grpcurl -plaintext -d '{"topic_name":"Basic Greetings","source_language":"en","target_language":"he"}' localhost:9000 backend.WordService/getWordPairs
```

## Key Features

- **Reactive Streaming**: All list endpoints return gRPC streams for efficient data transfer
- **Global Logging**: LoggingInterceptor logs all gRPC calls with timing and error details
- **Database Migrations**: Liquibase MongoDB manages schema evolution
- **Panache Reactive**: MongoDB operations use reactive patterns with Mutiny
