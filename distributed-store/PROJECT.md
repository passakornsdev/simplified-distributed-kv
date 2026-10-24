# Distributed Key-Value Store

Simplified Dynamo-inspired distributed key-value store for an applied distributed computing course. It is compared with the monolithic baseline in `../monolith-store`.

- Initial implementation: a runnable, package-renamed copy of the monolith's in-memory `GET` and `PUT` store. This provides the shared API baseline before distributed behavior is added.
- Operations: `GET` and `PUT` only, with the same API contract as the monolith (`PUT /kv/{key}` takes a plain-text body).
- Design: see [ARCHITECTURE.md](ARCHITECTURE.md).
- Stack: Java 25, Spring Boot 4, Maven, Docker Compose.
