# Distributed Key-Value Store

Simplified Dynamo-inspired distributed key-value store for an applied distributed computing course. It is compared with the monolithic baseline in `../monolith-store`.

- Deployment: `compose.yaml` starts five store nodes and an nginx load balancer; public requests
  enter through `http://localhost:8080`.
- Operations: `GET` and `PUT` only, with the same API contract as the monolith (`PUT /kv/{key}` takes a plain-text body).
- API: see the [OpenAPI specification](openapi.yaml).
- Design: see [ARCHITECTURE.md](ARCHITECTURE.md).
- Stack: Java 25, Spring Boot 4, Maven, Docker Compose.
