# Monolithic Key-Value Store

## Purpose

This project is a monolithic key-value store for an applied distributed computing course. It is the monolithic baseline used to compare the characteristics of a separate simplified distributed key-value store.

## Scope

This repository and its `monolith-store` module focus only on the monolithic implementation. The store will support two operations:

- `PUT` stores a value under a key.
- `GET` retrieves the value stored for a key.

Distributed-system features and concerns are out of scope for this project. Do not add distribution, replication, partitioning, consensus, inter-node communication, or related behavior here.

## API

The `monolith-store` module exposes a REST API with JSON responses:

| Operation | Endpoint | Request | Success response |
| --- | --- | --- | --- |
| PUT | `PUT /kv/{key}` | Plain-text string | `200 OK` with `{"key":"...","value":"..."}` |
| GET | `GET /kv/{key}` | None | `200 OK` with `{"key":"...","value":"..."}` |

Keys and values must be nonblank strings. Invalid requests receive `400 Bad Request` with
an error message. Retrieving a key that has not been stored receives `404 Not Found`.

## Storage Lifecycle

Values are stored in memory only and are discarded when the application stops. A `PUT` for
an existing key replaces its value.

## Container Image

Build and run the application with Docker from the `monolith-store` directory:

```sh
docker build -t monolith-store .
docker run --rm -p 8080:8080 monolith-store
```

## Technology and Structure

- Java 25
- Spring Boot 4
- Maven
- `monolith-store/`: application module

## Guidance for Future Agents

Keep changes focused on the monolithic key-value store and its `GET` and `PUT` operations. Preserve the project's role as a simple baseline; do not broaden its scope to implement distributed behavior.
