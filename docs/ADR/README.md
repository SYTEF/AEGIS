# Architecture Decision Records

## Purpose

An Architecture Decision Record (ADR) captures an important decision, its context, alternatives and consequences. ADRs preserve why AEGIS chose an approach so future contributors can evaluate it without reconstructing lost discussion.

ADRs are appropriate when a decision is difficult to reverse, affects multiple modules or quality attributes, introduces operational cost, defines a durable contract, or rejects a plausible alternative for a non-obvious reason. Routine local implementation details do not need an ADR.

## Decision process

1. Identify the concrete problem, constraints, requirements and affected quality attributes.
2. Describe viable options, including “do nothing/use the current design.”
3. Compare benefits, costs, risks, testability, security, observability, operations and migration/rollback.
4. Propose one decision with measurable consequences.
5. Obtain the required human/technical review before implementation.
6. Mark status and link the ADR from affected documentation/plan.
7. If the decision changes, create a superseding ADR; do not rewrite accepted history beyond minor corrections.

## Naming and status

Files use `ADR-NNN-short-kebab-title.md`, for example `ADR-001-modular-monolith.md`.

Allowed statuses:

- `Proposed`: under review; not authorization to implement;
- `Accepted`: approved and expected to guide implementation;
- `Rejected`: considered but not selected, with rationale;
- `Deprecated`: no longer recommended but may still exist;
- `Superseded by ADR-NNN`: replaced by a later decision.

Numbers are never reused. The index below is updated whenever an ADR is added or changes status.

## ADR template

```markdown
# ADR-NNN — Decision title

- Status: Proposed
- Date: YYYY-MM-DD
- Owners: <roles/names>
- Related requirements: REQ-...
- Supersedes: none
- Superseded by: none

## Context

What concrete problem are we solving? What constraints and evidence exist?

## Decision drivers

- Product need
- Simplicity and maintainability
- Security, testability and observability
- Reliability/performance
- Local operation and cost

## Considered options

### Option A

Benefits, costs and risks.

### Option B

Benefits, costs and risks.

## Decision

The selected option and precise scope.

## Consequences

Positive, negative and neutral consequences, including migration/rollback.

## Validation

How the decision will be tested or measured and when it should be revisited.
```

## Proposed future ADRs

The following titles are candidates, not accepted decisions:

| Candidate | Problem to decide | Expected timing |
| --- | --- | --- |
| ADR-002 PostgreSQL | Persistence fit, schema/migration strategy and local version | Before first durable catalog slice |
| ADR-003 RabbitMQ | Why async messaging is needed, topology, retry/DLQ and alternatives | Before integration phase |
| ADR-004 MinIO | Object storage need, lifecycle, local setup and alternatives | Before media phase |
| ADR-005 Playwright | Browser test role, project structure, browser matrix and alternatives | Before first frontend E2E suite |
| ADR-006 Authentication | Cookie/token/provider model, session lifecycle and security consequences | Before authentication phase |
| ADR-007 Optimistic concurrency | ETag versus explicit version contract | Before mutable catalog endpoint |
| ADR-008 Quality formula and policy versioning | Score dimensions, missing evidence, gates and governance | Before Quality Engine implementation |
| ADR-009 Observability telemetry backends | Collection/storage, local profile, retention and cost | Before full observability phase |

Candidate numbering may change until a file is created. Do not create every ADR in advance; create one when the decision is ready and needed.

## Technology bar

An ADR proposing Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch or microservices must answer:

1. What measured, concrete problem exists?
2. Why can the modular monolith/current stack not solve it more simply?
3. What new failure modes, security surface and operating cost result?
4. How will local reproducibility, tests and observability remain credible?
5. What is the migration and rollback path?

Without a convincing answer and explicit approval, the decision is **Rejected**.

## ADR index

| ADR | Status | Decision |
| --- | --- | --- |
| [ADR-001 Modular Monolith](ADR-001-modular-monolith.md) | Accepted | Start with explicit in-process modules and require measured evidence before distribution |

Future ADRs remain candidates until their decision is needed and reviewed.
