# AEGIS — Commerce Quality Engineering Platform

> Quality isn't the final step. It's part of the architecture.

AEGIS is a portfolio-grade product built to demonstrate professional Quality Engineering across the architecture and delivery lifecycle of a real application. It is not a test automation framework: it combines a commerce product with a quality and release intelligence product.

## Product domains

### AEGIS Commerce

A product catalog management system designed to evolve through authentication, authorization, products, categories, SKU, pricing, inventory, media, history, audit, search, integrations, events, retry and idempotency.

### AEGIS Quality Control Center

A control plane for releases, test executions, defects, security findings, performance results, quality metrics, risk and release decisions. A future Quality Engine will combine a transparent quality score with non-negotiable hard gates.

## Why AEGIS exists

AEGIS demonstrates that quality is a system property. Testability, security, observability, resilience and traceability are architectural concerns, not activities deferred until delivery. The intended professional audience includes QA Engineers, Quality Engineers, SDETs, Test Automation Engineers and Software Engineers in Test.

## Architecture direction

AEGIS starts as a **modular monolith**, formally accepted in [ADR-001](docs/ADR/ADR-001-modular-monolith.md), with clear boundaries:

- `auth`: identity, authentication and RBAC;
- `catalog`: products, categories, SKU, price and inventory;
- `media`: secure image intake and processing;
- `integration`: reliable communication with the External Sales Center Mock;
- `quality`: releases, test evidence, metrics, gates, risk and recommendations;
- `audit`: immutable records of security- and business-relevant actions.

Preferred future technologies are React and TypeScript, Java and Spring Boot, PostgreSQL, RabbitMQ, MinIO, Playwright, k6, Docker Compose, GitHub Actions, OpenTelemetry, Prometheus and Grafana. They are not implemented by this foundation and remain subject to Architecture Decision Records (ADRs).

The approved **planning baseline** for the future Phase 01 is JDK 25 LTS, Spring Boot 4.1.x, Maven, executable Jar, Maven coordinates `io.github.sytef:aegis` and base package `io.github.sytef.aegis`. GitHub Actions will begin with `permissions: contents: read`. These decisions do not mean Phase 01 has been implemented or authorized to start.

Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch and microservices are explicitly excluded unless a measured problem justifies them.

## Repository status

This repository is currently in **v0.1 Foundation**. It contains the official product and engineering documentation only. There is no frontend, backend, database, executable test suite or infrastructure yet.

## Documentation map

| Document | Purpose |
| --- | --- |
| [Project](docs/PROJECT.md) | Vision, users, scope and success criteria |
| [Requirements](docs/REQUIREMENTS.md) | Functional and non-functional requirements, rules and acceptance criteria |
| [Architecture](docs/ARCHITECTURE.md) | System boundaries, modules, flows and failure scenarios |
| [Data model](docs/DATA_MODEL.md) | Conceptual entities, ownership and relationships |
| [API specification](docs/API_SPEC.md) | Initial conceptual HTTP contracts |
| [Test strategy](docs/TEST_STRATEGY.md) | Quality Engineering strategy and test levels |
| [Security](docs/SECURITY.md) | Security principles and initial threat model |
| [Quality gates](docs/QUALITY_GATES.md) | Progressive gates, scoring, risk and hard blocks |
| [Observability](docs/OBSERVABILITY.md) | Logs, metrics, traces and failure investigation |
| [Roadmap](docs/ROADMAP.md) | Incremental, revisable product evolution |
| [Execution plans](PLANS.md) | Phase objectives, scope and definitions of done |
| [Agent constitution](AGENTS.md) | Operational rules for contributors and agents |
| [ADRs](docs/ADR/README.md) | Architecture decision process and index |
| [Glossary](docs/GLOSSARY.md) | Shared meaning of release, candidate, build, evidence, gate, risk and decision |

## Core quality policies

- Every material requirement must be traceable to tests, executions, evidence, defects and releases where applicable.
- A quality score informs decisions but never overrides a critical hard gate.
- Tests must not be deleted, skipped or weakened merely to obtain a green pipeline.
- A test must never be changed only to make it pass; first determine whether the defect is in the product, test, data, environment or requirement.
- Failures must be diagnosable through useful evidence and observability.
- Important and difficult-to-reverse decisions must be recorded as ADRs.

## Conceptual traceability

```text
Requirement -> Test Case -> Test Execution -> Evidence -> Defect -> Release
```

Identifiers are stable and human-readable, for example `REQ-CAT-001`, `TC-API-CAT-001`, `DEF-001` and release `v1.0.0`.

## Current constraints

- Documentation is authoritative for the foundation, but implementation details may evolve through ADRs.
- Threshold values in quality, security and performance policies are initial proposals and must be calibrated with evidence.
- Local execution instructions will be added when executable components first exist.

## Delivery targets

- **MPR — Minimum Portfolio Release:** proves the secure catalog, reproducible engineering path, layered quality evidence, traceability and hard gates without requiring every advanced differentiator.
- **v1.0 Full Vision:** retains Release Intelligence, validated Quality Score/risk/recommendation, Fault Lab and the complete planned observability demonstration.

The MPR prevents advanced features from making the portfolio impossible to finish; it does not lower applicable quality or security gates. See the [roadmap](docs/ROADMAP.md#mpr--minimum-portfolio-release).

## Contributing

Before changing the project, read [AGENTS.md](AGENTS.md), the relevant requirements and architecture sections, and the applicable phase in [PLANS.md](PLANS.md). Do not implement a roadmap phase without explicit human approval.

## License

No license has been selected. Until one is added, no reuse rights are granted by default.
