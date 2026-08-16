# AEGIS Project Definition

## Identity

- **Name:** AEGIS — Commerce Quality Engineering Platform
- **Tagline:** Quality isn't the final step. It's part of the architecture.

## Vision

Build a credible, inspectable product in which quality is designed into requirements, architecture, implementation, delivery and operations. AEGIS should show how a real commerce workflow and a Quality Control Center can produce evidence-based release decisions without reducing Quality Engineering to UI automation.

## Problem

Many demonstration projects present testing as a collection of scripts added after application development. They omit risk analysis, traceability, data quality, contracts, security, performance, resilience, observability and release governance. As a result, they do not demonstrate how quality decisions are made in a production-minded software system.

Teams also frequently consolidate release evidence manually across disconnected tools. Pass rates may be reported without business risk, critical findings can be hidden by aggregate scores and failures can be difficult to investigate because telemetry and evidence were not designed with testability in mind.

## Proposal

AEGIS consists of two connected product domains:

1. **AEGIS Commerce** — a real product catalog management system with identity, catalog, media, audit and reliable external integration capabilities.
2. **AEGIS Quality Control Center** — a system that consolidates requirements, test results, evidence, defects, security findings, performance metrics and observability signals per release.

A future **Quality Engine** will calculate a transparent Quality Score, classify risk and recommend a release decision. Critical rules remain hard blockers regardless of score.

## Objectives

- Deliver a coherent product rather than a test-only showcase.
- Embed testability, security and observability in architecture and acceptance criteria.
- Demonstrate layered testing: unit, component, integration, API, contract, end-to-end, security, performance, resilience, accessibility and data quality.
- Provide end-to-end traceability from requirement to release decision.
- Make failures reproducible and diagnosable using evidence, logs, metrics and traces.
- Evolve incrementally from a modular monolith and add operational complexity only when justified.
- Document important trade-offs through ADRs.

## Users and stakeholders

| Persona | Need |
| --- | --- |
| Catalog Administrator | Manage categories and complete product information safely |
| Inventory Operator | Maintain stock with validation and history |
| Media Operator | Upload and monitor product image processing |
| Release Manager | Understand release readiness, risk and blocking conditions |
| QA / Quality Engineer | Design coverage, run tests, attach evidence and investigate failures |
| Developer | Receive fast, actionable feedback and reproduce defects |
| Security Reviewer | Inspect findings, audit trails and unresolved release risk |
| Engineering Manager / Interviewer | Evaluate engineering decisions and the credibility of quality practices |
| External Sales Center | Consume catalog changes through a simulated downstream boundary |

Personas may be fulfilled by the same person in early versions. Authorization must still model their responsibilities explicitly.

## Product scope

### AEGIS Commerce

- Authentication, users, roles and authorization.
- Products, categories, SKU, price, stock, descriptions, images and search.
- Pagination, filters and deterministic sorting.
- Product history and security/business audit events.
- Image validation, storage and processing lifecycle.
- Reliable asynchronous integration with an External Sales Center Mock.
- Events, retries, idempotency and visible failure states.

### Quality Control Center

- Releases, suites, cases, runs, results and evidence references.
- Defects, security findings, performance metrics and quality metrics.
- Traceability from requirements through releases.
- Transparent Quality Score, risk level, gate evaluation and release recommendation.
- Human-owned final release decision with actor, time and rationale.
- Fault Lab scenarios for controlled resilience testing in non-production environments.

### Engineering scope

- Automated and exploratory test strategy.
- Security by Design, Observability by Design and Testability by Design.
- Local reproducibility and CI/CD quality gates when implementation begins.
- Documentation that evolves with behavior.

## Non-scope

The following are outside the initial product scope:

- Shopping cart, checkout, payment, order fulfillment and consumer storefront.
- Real third-party sales platforms; the first integration is a controlled mock.
- Multi-tenant SaaS billing or global marketplace features.
- Native mobile applications.
- Production deployment, high availability or disaster recovery commitments during the foundation phases.
- A general-purpose test management replacement for every team.
- AI-based release decisions without explainable deterministic policy.
- Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch or microservices without an approved, evidence-backed ADR.

## Differentiators

- A real product domain and a quality domain evolve together.
- Quality evidence is modeled as product data, not buried only in CI logs.
- Critical risk cannot be averaged away by a numerical score.
- The Fault Lab connects controlled failures to resilience testing and observability.
- Traceability supports both auditability and targeted regression selection.
- Architecture remains intentionally simple until scale or isolation requires change.

## Success criteria

Success is evaluated progressively. The [MPR checkpoint](ROADMAP.md#mpr--minimum-portfolio-release) proves the minimum professional core; by the v1.0 Full Vision, AEGIS should demonstrate:

1. A reproducible local environment with documented startup, seed and teardown paths.
2. Authenticated, authorized catalog and media workflows with audit and history.
3. Reliable, observable and idempotent external synchronization.
4. Automated checks at appropriate layers, with risk-based E2E coverage rather than E2E overuse.
5. Traceable requirements, test evidence and defects associated with a release.
6. A Quality Control Center that explains score inputs, hard blockers, risk and recommendation.
7. Demonstrable security, performance and resilience scenarios with retained evidence.
8. CI quality gates that are fast at pull request level and progressively deeper toward release.
9. Documentation and ADRs that match implemented behavior.
10. No known unresolved critical security finding or critical test failure in an approved release.

Initial quantitative targets are defined in [QUALITY_GATES.md](QUALITY_GATES.md); they are policies to calibrate, not vanity metrics.

## Professional objectives

The project is designed as evidence for QA Engineer, Quality Engineer, SDET, Test Automation Engineer and Software Engineer in Test roles. It should demonstrate:

- product and risk thinking;
- requirements analysis and acceptance criteria design;
- software architecture and modularity;
- test strategy across functional and non-functional qualities;
- automation maintainability and appropriate layer selection;
- API, data, integration and UI testing;
- secure engineering and threat awareness;
- performance and resilience analysis;
- observability-assisted investigation;
- CI/CD governance, defect communication and release reasoning.

## Product assumptions

- A single organization and a modest catalog are sufficient for the first complete release.
- PostgreSQL can remain the source of truth for transactional data.
- Asynchronous delivery is useful for the external integration, but synchronous in-process calls are preferred between internal modules.
- Object storage is justified for product images; binary image data should not be stored in transactional tables.
- A release recommendation is advisory until an authorized human records the final decision.

Assumptions must be validated during implementation. Fragile or disproven assumptions become requirements, risks or ADRs rather than implicit behavior.

## Foundation NEXUS review — 2026-08-16

### Findings

| Perspective | Finding |
| --- | --- |
| Product | AEGIS contains two connected products with credible actors and workflows: catalog operations generate real state/failure concerns, while the Control Center turns their quality evidence into release decisions. It is not defined as an automation framework. |
| Architecture | The modular-monolith boundary is proportionate. RabbitMQ and MinIO are deferred until integration/media requirements justify them; the worker may remain in the same codebase. No prohibited technology is planned by default. |
| QA | Testability, layered testing, traceability, evidence, defect lifecycle, non-functional testing and hard-gate integrity are present in requirements and architecture rather than appended only to CI. |
| Security | The foundation covers RBAC, object authorization, hostile uploads, secrets, evidence provenance, release-policy integrity and Fault Lab containment. The concrete identity/session and retention designs remain deliberately open. |
| DevOps | A local-first path is planned and avoids Kubernetes/cloud dependency. There is no executable environment yet, so reproducibility is a Phase 01 acceptance criterion rather than a current capability. |
| Observability | Critical flows define logs, metrics, traces, IDs, health semantics, dashboards, alerts and a QA investigation path. Telemetry storage/backends and retention are unresolved. |
| Red Team | The most fragile assumptions are trustworthy quality-source provenance, realistic score calibration, sufficient single-person separation of duties, reliable production denial for Fault Lab, modest local resource needs and the external mock's representativeness. |

### Decisions

- The foundation is documentation-only; it makes no implementation, security-certification or production-readiness claim.
- Start as a modular monolith under accepted [ADR-001](ADR/ADR-001-modular-monolith.md), with explicit module APIs and PostgreSQL as the intended transactional source of truth.
- Introduce MinIO for approved image needs and RabbitMQ for reliable external integration only in their respective approved phases.
- Keep quality policy versioned and explainable; hard blocking rules override Quality Score.
- Automation recommends; an authorized human records the final release decision.
- Phase 01 remains a minimal executable backend/build/health proof with no database, frontend or business CRUD.

### Risks

- The breadth of documented future capability may be mistaken for committed scope or completed behavior.
- Draft performance thresholds and score weights may create false precision before baselining.
- The Quality Control Center could expand into a generic test-management product and distract from AEGIS release needs.
- A portfolio operated by one person cannot fully prove segregation of duties even when RBAC models it.
- Evidence, telemetry and audit retention may create privacy, storage and access costs not yet quantified.
- Local PostgreSQL, RabbitMQ, MinIO and observability components together may exceed a reviewer's practical resource budget.
- There is no selected license, hosted-environment threat model or public-operation commitment.

### Recommendations

- Obtain separate human authorization for Phase 01 only, using the accepted ADR-001 and approved JDK 25 LTS / Spring Boot 4.1.x / Maven baseline.
- Label future/draft metrics prominently in every implemented UI and release output until calibrated.
- Validate evidence provenance and build identity before implementing any score.
- Finalize authentication, role matrix, retention and exception authority immediately before their relevant phases.
- Measure local resource use as each dependency is introduced and provide lightweight profiles rather than deploying the entire future stack at once.
- Re-run this multi-perspective review at every milestone and convert disproven assumptions into requirements, risks or ADRs.

## Governance

- [REQUIREMENTS.md](REQUIREMENTS.md) defines intended behavior.
- [ARCHITECTURE.md](ARCHITECTURE.md) defines boundaries and dependency rules.
- [TEST_STRATEGY.md](TEST_STRATEGY.md), [SECURITY.md](SECURITY.md), [QUALITY_GATES.md](QUALITY_GATES.md) and [OBSERVABILITY.md](OBSERVABILITY.md) define cross-cutting policies.
- [ROADMAP.md](ROADMAP.md) communicates direction; [../PLANS.md](../PLANS.md) defines approval-ready phases.
- [ADR records](ADR/README.md) explain important decisions and supersede earlier assumptions when accepted.
- [GLOSSARY.md](GLOSSARY.md) defines cross-document release and evidence terminology.
