# Distributed Key-Value Store

Simplified Dynamo-inspired distributed key-value store for an applied distributed computing course. It is compared with the monolithic baseline in `../monolith-store`.

- Deployment: `k8s/distributed-store.yaml` starts five store nodes and a Kubernetes Service; public
  requests enter through `http://localhost:8080` when the service is port-forwarded.
- Operations: `GET` and `PUT` only, with the same API contract as the monolith (`PUT /kv/{key}` takes a plain-text body).
- API: see the [OpenAPI specification](openapi.yaml).
- Design: see [ARCHITECTURE.md](ARCHITECTURE.md).
- Stack: Java 25, Spring Boot 4, Maven, Docker, Kubernetes.
