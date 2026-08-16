# AEGIS Roadmap

## How to read this roadmap

This roadmap communicates intended product evolution, not an immutable delivery promise. Version labels describe capability milestones and may be split, reordered or rescoped after evidence and human review. A capability is complete only when its applicable acceptance criteria and Definition of Done are met; a version name alone does not indicate implementation.

Detailed approval-ready phases are in [PLANS.md](../PLANS.md). Architecture changes that are difficult to reverse belong in [ADRs](ADR/README.md).

## Guiding sequence

- Establish one reliable vertical slice before broad feature count.
- Build synchronous catalog value before asynchronous integration complexity.
- Add security and observability with the capabilities they protect, not only in late milestones.
- Test every layer when its boundary becomes real.
- Build the Quality Control Center from actual AEGIS evidence rather than speculative dashboards.
- Calibrate score/gates with data before presenting them as intelligence.

## Milestones

### v0.1 — Foundation

**Outcome:** official product, requirements, architecture, data, API, quality, security, observability and execution governance baseline.

Includes:

- project vision, scope, personas and success criteria;
- numbered requirements and conceptual contracts;
- modular-monolith boundaries and initial threat/failure models;
- layered Quality Engineering strategy and progressive gates;
- conceptual data model, roadmap, plans and ADR process;
- operational rules for contributors and agents.

Does not include executable application code or infrastructure. Documentation questions and draft thresholds remain explicit rather than fabricated as settled facts.

### v0.2 — Catalog

**Outcome:** first product-value vertical slices for categories and products as a **LOCAL DEVELOPMENT PREVIEW**.

Candidate scope:

- minimal reproducible backend and database foundation;
- create/read/update/deactivate product and category capabilities;
- SKU, money, stock, validation, pagination/filter and concurrency rules;
- history/audit fundamentals;
- executable API contract and layered catalog tests;
- minimal CI feedback and telemetry appropriate to implemented flows.

Media, external messaging and full browser UI remain out until their milestones. Start with a deliberately small Phase 01 before completing this milestone.

Until v0.3 authentication/RBAC is complete:

- catalog mutation cannot be claimed as secure or authorization-complete;
- the application must bind to/localize access for development and must not be publicly exposed;
- any development identity is test scaffolding, not authentication evidence;
- authorization acceptance criteria remain pending;
- an External Exposure Gate blocks deployment/hosting beyond the approved local/isolated environment.

### v0.3 — Authentication

**Outcome:** identities and server-enforced RBAC protect existing catalog behavior.

Candidate scope:

- chosen authentication/session mechanism through ADR;
- users, roles/permissions and deactivation/revocation;
- catalog and administrative authorization matrix;
- authentication abuse controls and security audit;
- negative API/security tests and documented local bootstrap.

MFA or external identity provider is included only if the threat/deployment model justifies it.

This milestone must close every authorization criterion intentionally left pending by the v0.2 local preview before the catalog can be externally exposed or described as access-controlled.

### v0.4 — Media

**Outcome:** safe product image lifecycle.

Candidate scope:

- MinIO decision and local dependency;
- restricted upload, validation, private storage and metadata;
- processing lifecycle, variants, failure/retry and cleanup;
- product association/order and authorized retrieval;
- hostile upload, integration, resource and observability tests.

Do not accept SVG or general files without a concrete requirement and updated threat model.

### v0.5 — Integration

**Outcome:** catalog changes reach an External Sales Center Mock reliably and observably.

Candidate scope:

- versioned event contract and transactional outbox;
- RabbitMQ topology based on an ADR;
- integration worker and controlled downstream mock;
- bounded retry, idempotency, terminal failure, reconciliation and replay;
- contract, restart, duplicate and failure-path tests;
- queue/outbox/delivery dashboards and alerts.

This milestone deliberately introduces distributed failure only after core catalog behavior is stable.

### v0.6 — Quality Engineering

**Outcome:** Quality Control Center ingests and presents traceable evidence from real AEGIS test sources.

Candidate scope:

- releases, suites/cases, runs/results and evidence metadata;
- source/build attribution and idempotent ingestion;
- defects, security findings and performance result normalization;
- requirement-to-release traceability and missing-link views;
- accessible initial Quality Control Center interface;
- Playwright critical journeys and documented exploratory charters.

This milestone does not claim an intelligent score; it establishes trustworthy evidence first.

Quality Score, risk classification and Engine recommendation are `NOT_APPLICABLE` through this milestone. Applicable hard gates and evidence freshness still govern candidate review and can block release.

### v0.7 — Quality Engine

**Outcome:** explainable, versioned release intelligence over trustworthy evidence.

Candidate scope:

- versioned hard gate policy and immutable evaluations;
- draft Quality Score implementation with contributing dimensions;
- risk classification and recommendation reasons;
- stale/missing/error evidence behavior;
- authorized human decision and time-bounded exception workflow;
- calibration tests and adversarial cases that attempt to hide critical risk.

Score remains experimental until its outputs are evaluated against representative scenarios.

### v0.8 — Security & Performance

**Outcome:** deeper, reproducible non-functional assurance and hardened implemented boundaries.

Candidate scope:

- updated threat model and targeted manual security review;
- supply-chain, source, dependency and artifact scanning maturity;
- k6 reference workload, dataset, baseline and calibrated thresholds;
- API abuse, upload, ingestion and authorization hardening;
- dashboard evidence and Quality Control Center integration;
- performance/security release gates with justified exception policy.

Thresholds are promoted to hard policy only when environments and measurements are valid.

### v0.9 — Observability & Fault Lab

**Outcome:** controlled failure experiments prove diagnosis, containment and recovery.

Candidate scope:

- OpenTelemetry correlation/tracing for critical flows;
- Prometheus metrics and Grafana operational/quality dashboards;
- actionable alerts and exercised investigation runbooks;
- permission-protected, non-production Fault Lab with TTL/emergency stop;
- downstream, latency, HTTP 500, database, media and queue fault scenarios;
- hypotheses, traces, results and recovery evidence linked to releases.

Fault injection remains off and unavailable in production by design.

## MPR — Minimum Portfolio Release

The MPR is a capability checkpoint designed to prove the professional core even if advanced features need more time. It is not a second architecture or permission to lower quality. It may be declared only when the exact included scope is documented and its applicable gates pass.

Minimum evidence of the core:

- reproducible local startup/test path from a clean clone;
- authenticated and authorized product/category management backed by PostgreSQL;
- validation, history/audit, concurrency and safe error behavior;
- one reliable, idempotent and observable external-integration flow, if integration is included in the declared MPR scope;
- layered unit/component/integration/API/contract coverage and a small critical UI journey when UI is included;
- security baseline, accessible critical UI, correlation/diagnostics and progressive CI gates;
- release candidate/build attribution, evidence summary, traceability and hard-gate results without requiring a numerical score.

The MPR deliberately does **not** require the future Quality Score, complete Release Intelligence UI, full Fault Lab scenario catalog or full observability stack. Those remain in the Full Vision and may be stretch goals if they threaten completion. Features already finished before the MPR remain included and must meet their gates.

The preferred checkpoint is after trustworthy evidence-summary capability exists, but roadmap learning may change order or reduce non-core breadth with human approval.

### v1.0 — Full Vision Portfolio Release

**Outcome:** a coherent, reproducible Full Vision portfolio release demonstrating a real product and professional Quality Engineering, including the advanced differentiators retained below.

Candidate scope:

- accessible polished Commerce and Quality Control Center critical journeys;
- documented local bootstrap, demo, test and failure-investigation paths;
- reproducible artifacts and progressive GitHub Actions gates;
- satisfied Must requirements or explicit scope decisions;
- current security, performance, resilience and observability evidence;
- reviewed ADRs, architecture, operations limitations and residual risks;
- release score/risk/recommendation and human final decision.
- controlled Fault Lab experiments and Release Intelligence backed by validated evidence.

“Production” here means production-minded portfolio quality. Actual public hosting, SLA, legal/privacy compliance and 24/7 operations require a separate deployment assessment.

## Cross-cutting tracks

Every milestone evaluates:

- requirements and traceability;
- secure design/threat changes;
- testability and appropriate automated/manual coverage;
- data integrity/migration;
- structured diagnostics and operational failure behavior;
- documentation/ADR consistency;
- local reproducibility and contributor experience.
- External Exposure Gate status while real authentication is absent.

Accessibility begins with any UI, not only v0.8. Security, testing and observability mature continuously even when a milestone gives one area special emphasis.

## Decision checkpoints

Human approval is required before starting each phase in [PLANS.md](../PLANS.md). At each milestone boundary, review:

1. Does the next capability solve a product/quality problem?
2. Are current boundaries healthy enough to extend?
3. What new security, data and operational risks appear?
4. What evidence proves the milestone and what remains unknown?
5. Should roadmap order or scope change based on learning?

## Explicitly deferred complexity

No roadmap milestone implicitly authorizes Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch or microservices. If a future measurement suggests one, compare simpler alternatives and document the decision through ADR before planning implementation.

## Current open questions

- Which minimal Phase 01 vertical slice best validates the Java/Spring Boot direction without overbuilding?
- Does v1.0 need a deployed public demo, or is a reproducible local demo the acceptance target?
- Which browser/authentication model and role-separation depth fit the portfolio goal?
- What hardware/data profile makes performance claims reproducible for reviewers?
- What evidence retention is viable without making the repository or local stack heavy?
- Which integrations with external issue/test systems are unnecessary for v1.0 and should remain simulated?
