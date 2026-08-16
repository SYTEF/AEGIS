# ADR-001 — Modular Monolith

- Status: Accepted
- Date: 2026-08-16
- Decision owners: NEXUS / repository owner
- Related requirements: NFR-MAINT-001, NFR-PORT-001, NFR-TEST-001
- Supersedes: none
- Superseded by: none

## Context

AEGIS must demonstrate a real Commerce product and a Quality Control Center while remaining understandable, testable and reproducible on a contributor's local machine. The project does not currently have measured scale, independent teams, deployment-frequency conflicts, isolation requirements or operational constraints that justify distributed services.

Starting with microservices would add network failure, distributed transactions, contract/version coordination, deployment topology, security boundaries and observability costs before the product has validated those needs. Those costs would work against the project's goals of professional Quality Engineering, low cognitive load and incremental delivery.

## Decision

AEGIS will start as a **modular monolith**.

- Business capabilities are organized into explicit modules with owned domain models, application APIs and persistence boundaries.
- Cross-module calls use documented public application interfaces or published events. Direct access to another module's repository or table is prohibited.
- Module dependencies must be acyclic and enforceable by structure and automated architecture checks when code exists.
- Internal calls are synchronous and in-process by default.
- Asynchronous messaging is introduced only for a concrete reliability, recovery or external-integration requirement.
- One codebase and application artifact are preferred initially. A worker may run as a separate process profile from the same codebase when asynchronous integration requires it; this does not make it a microservice.
- Local execution must remain possible without Kubernetes, a cloud account or a service mesh.

The initial conceptual modules are `auth`, `catalog`, `media`, `integration`, `quality` and `audit`. An application/query composition boundary may combine module-owned read models for client representations without creating module-to-module cycles.

## Alternatives Considered

### Microservices from the beginning

Rejected. No measured scaling, ownership or deployment requirement offsets the additional network, consistency, security, test and operational complexity.

### Unstructured monolith

Rejected. A single deployable without enforced boundaries would make ownership unclear, encourage direct table coupling and make later evolution harder to test or reason about.

### Serverless functions per capability

Rejected for the initial system. It would distribute behavior and local execution without a demonstrated workload or operational benefit.

### Modular monolith

Accepted. It provides explicit boundaries and realistic engineering constraints while preserving simple execution, transactions, refactoring and test feedback.

## Consequences

### Positive

- Lower cognitive and operational cost.
- Straightforward local startup and debugging.
- Strong transactional consistency within module-owned operations.
- Faster unit, component and integration feedback.
- Clear capability boundaries can be validated before any future extraction.
- One codebase supports coherent cross-cutting security, observability and quality policies.

### Negative

- Module boundaries require deliberate enforcement because process and database proximity make shortcuts easy.
- A single application artifact can couple release cadence and scaling.
- Cross-module transactions may be tempting and must remain exceptional and explicit.
- A separately running integration worker still shares code/artifact evolution with the application.

### Neutral

- PostgreSQL may be one logical database, but modules own their tables and migrations.
- RabbitMQ and MinIO remain external dependencies only when their approved phases introduce them.
- Modular Monolith does not forbid future extraction; it requires evidence first.

## Risks

- The codebase could degrade into a tightly coupled monolith if public module APIs and dependency direction are not enforced.
- A central `quality` capability could become oversized unless internal sub-boundaries remain explicit.
- Shared technical utilities could accumulate business rules and create hidden coupling.
- Cross-module database foreign keys or transactions could erode lifecycle ownership.
- Contributors could mistake separate runtime profiles for independent services.

Mitigations include architecture tests, module-owned migrations, small public APIs, dependency review, ADRs for cross-module persistence and regular boundary audits.

## Validation

The decision is validated incrementally by confirming that:

1. Phase 01 can build, test and run one application artifact locally with a documented command path.
2. Module dependency rules can be expressed and automatically checked without empty speculative modules.
3. Catalog behavior can be implemented without direct access to other module repositories.
4. Media composition and integration publication preserve acyclic ownership.
5. Local resource usage remains practical as approved dependencies are introduced.
6. Component/integration tests can isolate module boundaries and exercise real adapters where needed.

Evidence that the modular monolith is difficult to maintain must identify the concrete boundary and measured failure; general preference for microservices is not evidence.

## Revisit Conditions

Revisit this decision only when one or more measured conditions exist:

- a module requires materially different independent scaling;
- independent teams need incompatible release cadences or ownership isolation;
- a failure-isolation or security boundary cannot be achieved reasonably in-process;
- deployment size/startup/resource behavior creates a demonstrated operational constraint;
- a module has stable contracts and data ownership but the monolith prevents a required capability;
- local and CI evidence shows that extraction benefits exceed network, consistency and operational costs.

Any extraction requires a superseding ADR with migration, rollback, security, test, data and observability consequences. Kubernetes, service mesh or other orchestration is a separate decision and is not implied by service extraction.
