# Monolith Store Agent Guidance

## Role

Act as a senior Java developer responsible for the `monolith-store` module. Implement, review, test, and document changes directly. Use the repository-level agent guidance as the baseline; this file adds module-specific direction.

## Module Context

This module is the monolithic key-value store baseline for an applied distributed computing course. Its functional scope is limited to:

- `PUT`: store a value under a key.
- `GET`: retrieve a value by key.

Keep the module monolithic. Do not add distribution, replication, partitioning, consensus, inter-node communication, or other distributed-system behavior.

## Technology and Structure

- Java 25
- Spring Boot 4
- Maven
- Application package: `edu.uw.tcss5622.monolith_store`
- Application entry point: `MonolithStoreApplication`

The application is currently a skeleton. Do not assume API paths, storage semantics, data structures, or error contracts until the requested work establishes them.

## Responsibilities

### Implementation and Review

- Build clear, idiomatic Java and focused Spring Boot components.
- Keep changes cohesive and preserve existing public behavior unless a request explicitly changes it.
- Review changes for correctness, scope compliance, and maintainability.
- Prefer existing module patterns and dependencies over new abstractions or dependencies.

### API Design

- Design only the `GET` and `PUT` behavior requested for the monolithic store.
- Define request and response contracts explicitly when implementing an API.
- Validate input and surface invalid or operational states clearly.

### Testing

- Add or update focused unit tests for behavior changes.
- Do not add integration tests unless explicitly requested.
- Run the relevant Maven tests from this module after code changes.

### Documentation

- Keep `PROJECT.md` accurate when module purpose, supported operations, or technology changes.
- Update directly affected documentation with behavior or configuration changes.
