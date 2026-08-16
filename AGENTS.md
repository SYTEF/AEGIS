# AEGIS Operational Constitution

## Authority and scope

This file governs human and automated agents working anywhere in the AEGIS repository. More specific instructions may add constraints but must not weaken this constitution, security policy, requirements or quality gates. When instructions conflict or scope is unclear, stop and request human direction.

The repository is intentionally incremental. A roadmap entry is not authorization to implement it. Work only on the explicitly approved phase/task.

## Core obligations

- Preserve AEGIS as a product with embedded Quality Engineering, not a test-script showcase.
- Prefer the simplest design that satisfies approved requirements.
- Maintain modular-monolith boundaries; distributed complexity requires a concrete problem and approved ADR.
- Never mask errors, fabricate evidence or claim an unimplemented capability exists.
- Treat security, testability, observability, data integrity and accessibility as design concerns.
- Preserve user work and unrelated changes; do not perform destructive Git/filesystem operations without explicit authorization.
- Keep secrets, credentials and personal/sensitive data out of source, logs, examples and artifacts.

## Before altering anything

An agent must:

1. Confirm repository root, current worktree state and task scope.
2. Read this file, the relevant phase in [PLANS.md](PLANS.md), and applicable documents under `docs/`.
3. Identify requirement IDs and acceptance criteria affected; do not invent behavior that contradicts them.
4. Check [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and relevant [ADRs](docs/ADR/README.md) for boundaries and decisions.
5. Evaluate Quality Engineering impact: test layers, data, evidence, traceability, regressions and failure diagnosis.
6. Evaluate security impact: authentication/authorization, validation, secrets, sensitive data, abuse and supply chain.
7. Evaluate data/migration, API/event compatibility, observability and local-execution impact where applicable.
8. Inspect existing code/tests/configuration before proposing a pattern. Do not assume placeholder architecture is implemented.
9. Clarify with a human before a material scope expansion, destructive action, public/external side effect or difficult-to-reverse decision.

For documentation-only work, “applicable” checks still include cross-document consistency and relative links; executable checks are not fabricated.

## During a change

- Make small, cohesive, reviewable changes tied to approved scope.
- Follow existing conventions and module ownership; avoid direct access to another module's persistence internals.
- Keep business policy separate from framework/I/O details and expose controllable boundaries for tests.
- Validate inputs at trust boundaries and authorize protected actions server-side.
- Use safe error contracts; never expose stack traces, secrets or sensitive payloads.
- Preserve backward compatibility or document/version an intentional break.
- Make async behavior idempotent, retry bounded and failure state visible.
- Add logs/metrics/traces intentionally, with correlation and redaction; avoid high-cardinality metrics.
- Update tests with behavior changes at the lowest effective layer plus necessary integration/user coverage.
- Keep test outcomes deterministic. Do not add sleeps, catch-and-ignore or uncontrolled global retry.
- Do not add a technology, abstraction, table, module or service without a requirement/use case.
- Record difficult-to-reverse decisions as ADRs; do not silently decide through code alone.
- Never edit generated dependencies/build output as source.

## Critical test integrity rule

Tests must not be deleted, ignored, skipped, quarantined, broadly retried, weakened or have assertions removed merely to obtain a green pipeline.

Never modify a test only to make it pass. Investigate whether the cause is:

- a product defect;
- a legitimate requirement/contract change;
- an incorrect or obsolete test expectation;
- test implementation/data nondeterminism;
- environment/tooling failure.

A legitimate test change must explain which cause applies and preserve or improve risk coverage. Quarantine follows [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md#flaky-test-policy), requires an owner/expiry and never counts as pass.

## Prohibited shortcuts

Without explicit requirement, evidence and approved ADR, do not introduce Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch or microservices.

Also do not:

- disable validation, authorization, TLS/security checks or quality gates to simplify development;
- use real secrets or production/personal data for tests/demos;
- make the Fault Lab reachable in production;
- treat mock-only tests as proof that real persistence/messaging/storage behavior works;
- interpret missing/stale quality evidence as passing;
- let Quality Score override a hard blocking rule;
- claim “exactly once” across distributed boundaries;
- perform opportunistic unrelated refactors during a focused task;
- commit, push, deploy, publish or contact external systems unless explicitly requested.

## After a change

An agent must:

1. Run the smallest complete set of applicable validation, then broader checks proportional to risk.
2. Review the complete diff for unintended edits, generated files, secrets and scope creep.
3. Confirm errors/failures were resolved rather than hidden.
4. Verify requirement/test/evidence traceability where implemented.
5. Recheck authorization, validation, data integrity, backward compatibility and failure modes.
6. Recheck telemetry, correlation, redaction, health and operational behavior where relevant.
7. Update documentation, examples, ADRs and plans when behavior/decisions changed.
8. Report validations actually run, results, unrun checks, assumptions, risks and follow-up work.
9. Do not commit or push unless the human explicitly requested it.

## Definition of Done

A change is done only when all applicable conditions are met:

### Scope and correctness

- Approved objective and acceptance criteria are satisfied without unrelated expansion.
- Requirement IDs and domain rules are reflected in implementation and tests.
- Boundary/error/concurrency/failure behavior is considered, not only the happy path.

### Build and static quality

- Build/type checking succeeds.
- Formatting and lint/static analysis succeed.
- No unjustified dependency, warning suppression or generated noise is introduced.

### Tests and evidence

- Appropriate unit, component, integration, API, contract, E2E and non-functional checks pass as applicable.
- New/changed risk has coverage at the lowest effective layer.
- Test data is isolated and no failure is masked by retry/skip/weakened assertion.
- Relevant execution evidence identifies build/environment and failures are diagnosable.

### Security

- Authentication, authorization, validation, secrets, abuse and dependency considerations were reviewed.
- Applicable security checks pass and no unresolved hard blocker exists.
- Sensitive data is absent from logs/errors/evidence/source.

### Documentation and decisions

- API/event/data/behavior documentation is current.
- Important trade-offs are captured in an ADR.
- Relative links and cross-document terminology remain consistent.

### Observability and operations

- Critical new outcomes/failures have proportionate structured telemetry and correlation.
- Health, retry/recovery, alerts/runbooks and local execution are updated where applicable.
- No high-cardinality or sensitive telemetry is introduced.

### Review

- Full diff and repository status were reviewed.
- All executed and unexecuted validations, open risks and limitations are honestly reported.

For documentation-only changes, executable build/lint/test items are `not applicable`, but link, consistency, scope and diff review are mandatory.

## Change report template

Use a concise handoff:

```text
Scope:
Requirements:
Files changed:
Validation performed:
Validation not performed and why:
Security/observability impact:
Risks and open questions:
```

Never report a check as passed if it was not executed.

## Documentation authority

- [docs/PROJECT.md](docs/PROJECT.md): product intent and scope.
- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md): behavior and acceptance baseline.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): boundaries and dependency rules.
- [docs/API_SPEC.md](docs/API_SPEC.md) and [docs/DATA_MODEL.md](docs/DATA_MODEL.md): conceptual contracts/models.
- [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md), [docs/SECURITY.md](docs/SECURITY.md), [docs/QUALITY_GATES.md](docs/QUALITY_GATES.md), [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md): cross-cutting policy.
- [docs/ROADMAP.md](docs/ROADMAP.md): revisable direction.
- [PLANS.md](PLANS.md): approved-work candidate phases.
- Accepted ADRs: reasons and consequences for architectural decisions. A superseding ADR must say what it replaces.

When documents disagree, do not select the convenient rule. Identify the conflict, assess risk and request/record a deliberate resolution.
