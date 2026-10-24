# EventFlow — Reliable Webhook Delivery Platform

**EventFlow** is a Java-based webhook and event delivery platform inspired by modern event infrastructure systems. Its goal is to provide durable event ingestion, asynchronous delivery, configurable retries, failure recovery, and delivery observability.

The project is being developed incrementally, with a focus on backend engineering, distributed systems, reliability, testing, and deployment automation.

> **Project status:** Initial development. Event ingestion and retrieval APIs are under implementation and verification. Features listed as planned are not yet considered complete.

## Objectives

- Accept events through REST APIs and persist them reliably.
- Decouple event ingestion from downstream HTTP delivery.
- Support retries, dead-letter handling, and manual event replay.
- Prevent duplicate processing where possible through idempotency.
- Provide delivery history, event status, and operational metrics.
- Automate testing, container builds, and deployment workflows.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Backend framework | Spring Boot |
| Build tool | Maven |
| Database | PostgreSQL |
| Database migrations | Flyway |
| Messaging | Apache Kafka |
| API documentation | Springdoc OpenAPI |
| Containerization | Docker |
| Orchestration | Kubernetes |
| CI/CD | GitHub Actions |
| Cloud deployment | AWS |
| Testing | JUnit 5, Spring Boot Test, Testcontainers |
| Observability | Spring Boot Actuator, Micrometer, Prometheus, Grafana |

Technologies and features will be integrated and verified as development progresses.

## Architecture (Planned)

```text
API Client
    |
    v
Spring Boot REST API
    |
    v
PostgreSQL
    |
    v
Transactional Outbox
    |
    v
Apache Kafka
    |
    v
Delivery Workers
    |
    v
Registered Webhook Endpoints

Monitoring:
Application Metrics + Structured Logs + Dashboards
```

The transactional outbox will help coordinate database persistence and message publication. Delivery workers will handle outbound HTTP requests, retry policies, and delivery status updates.

## Current API Design

### Create an event

`POST /api/v1/events`

Example request:

```json
{
  "eventType": "order.created",
  "payload": {
    "orderId": "ORD-1001",
    "amount": 1499,
    "currency": "INR"
  }
}
```

Expected behavior: validate the request, persist the event, and return an event identifier with HTTP `201 Created`.

### Retrieve an event

`GET /api/v1/events/{eventId}`

Expected behavior: return the stored event or HTTP `404 Not Found` if the event does not exist.

These endpoints are currently being verified; their expected behavior should not be interpreted as a claim that they are already working.

## Local Development

### Prerequisites

- Java 21
- Docker Desktop
- Git
- Maven Wrapper included in the repository

### Start infrastructure

```powershell
docker compose up -d
docker compose ps
```

### Build and test

```powershell
.\mvnw.cmd clean test
```

### Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Default local API address:

`http://localhost:8080`

API documentation, when the application starts successfully:

`http://localhost:8080/swagger-ui.html`

Database connection details and development-only credentials are configured for local use. Never commit real passwords, access tokens, AWS credentials, or other secrets.

## Engineering Roadmap

- [x] Initial project scaffold
- [ ] Verify event creation and retrieval APIs
- [ ] PostgreSQL persistence and Flyway migration verification
- [ ] Automated unit and integration tests
- [ ] Transactional outbox and Kafka publishing
- [ ] Asynchronous delivery workers
- [ ] Retry policies and dead-letter handling
- [ ] Idempotency and safe replay
- [ ] Destination routing and rate limiting
- [ ] Authentication and webhook signature verification
- [ ] Docker image and CI pipeline
- [ ] Kubernetes manifests and Helm chart
- [ ] AWS deployment within a controlled budget
- [ ] Metrics, dashboards, and load testing
- [ ] Architecture documentation and demonstration

## Reliability Goals

The implementation will be evaluated against concrete failure scenarios:

- Database unavailable during event ingestion
- Duplicate events and repeated delivery attempts
- Downstream endpoints returning HTTP 500 or timing out
- Worker restart during message processing
- Retry exhaustion and dead-letter handling
- Recovery after a temporary service outage

Performance results will be documented only after repeatable tests, with test conditions and measured results.

## Development Principles

- Version-controlled database migrations
- Automated tests and reproducible builds
- Explicit transaction boundaries
- Structured logs and traceable event identifiers
- Secure configuration and secret management
- Documented architecture decisions
- Cost-conscious cloud infrastructure

## License

License to be selected before public release.

## Author

Developed as a backend engineering portfolio project to explore reliable event processing, Java backend development, distributed systems, and production-oriented DevOps practices.
