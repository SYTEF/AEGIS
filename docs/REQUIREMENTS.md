# AEGIS Requirements

## Purpose and conventions

This document is the initial product requirements baseline. It describes intended capabilities; roadmap version assignment does not mean a capability is already implemented.

Requirement IDs are stable:

- `REQ-<DOMAIN>-NNN`: functional requirement;
- `NFR-<QUALITY>-NNN`: non-functional requirement;
- `BR-<DOMAIN>-NNN`: business rule.

Priority uses **Must**, **Conditional Must**, **Should** and **Could**. A Conditional Must becomes mandatory when its named capability/maturity applies and is explicitly `NOT_APPLICABLE` before then. Acceptance criteria use concise Given/When/Then language and must be refined with examples before implementation. A requirement is not complete merely because one happy-path automated test passes.

## Functional requirements

### Identity and access (`AUTH`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-AUTH-001 | Must | The system shall authenticate an active user with supported credentials. | Given valid credentials for an active user, when authentication succeeds, then a time-limited session/token is issued without exposing secrets; invalid credentials return a generic unauthorized response. |
| REQ-AUTH-002 | Must | The system shall end or invalidate an authenticated session. | Given an authenticated user, when logout or revocation completes, then the affected session cannot access protected endpoints. |
| REQ-AUTH-003 | Must | Protected capabilities shall require authentication. | Given an anonymous or expired session, when a protected operation is attempted, then it is rejected as unauthenticated and no state changes. |
| REQ-AUTH-004 | Must | Authorization shall be role- and permission-based. | Given users with different permissions, when they invoke the same operation, then only permitted actors succeed and denials are auditable. |
| REQ-AUTH-005 | Must | Authorized administrators shall create/disable users and add/remove role assignments. | Every change validates actor/target, protects the last active administrator, revokes affected sessions after a critical permission change and emits the required fail-closed audit event. |
| REQ-AUTH-006 | Should | Security-sensitive session events shall be visible to the affected user or an administrator. | Successful and failed login indicators contain time and safe client context without recording credentials or raw tokens. |

#### Minimum conceptual role matrix

This is a planning baseline, not an implemented or exhaustive authorization policy. Exact permissions are reviewed before Phase 03.

| Role | Intended responsibilities | Minimum conceptual permissions |
| --- | --- | --- |
| `ADMIN` | Create/disable users, assign/remove roles, inspect audit and perform platform administration | `admin:users`, `admin:roles`, `audit:read`; business permissions are granted explicitly rather than implied |
| `QUALITY_MANAGER` | Manage releases/evidence, evaluate gates and record authorized release decisions | `quality:read`, `quality:write`, `release:decide`; `quality:policy:admin` is separate and must be granted explicitly |
| `OPERATOR` | Operate catalog and media workflows | `catalog:read`, `catalog:write`, `catalog:history:read`, `media:read`, `media:write` |
| `VIEWER` | Read authorized Commerce and quality summaries without mutation | `catalog:read`, `media:read`, `quality:read` |

`quality:policy:admin` controls changes to score formulas, risk policies and gate definitions. It is never implied by generic `quality:write`. CI/evidence-ingestion identities are scoped service identities, not human roles, and do not receive release-decision or policy-administration permission.

### Catalog (`CAT`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-CAT-001 | Must | Authorized users shall create a product with SKU, name, description, price, stock and category assignment. | Valid input creates one identifiable product; invalid fields yield field-level errors and no partial product. |
| REQ-CAT-002 | Must | Authorized users shall retrieve a product by its stable identifier. | An existing visible product returns its current representation; an unknown identifier returns not found without leaking restricted data. |
| REQ-CAT-003 | Must | Authorized users shall update allowed product attributes using concurrency protection. | A current version updates atomically and records history; a stale version returns a conflict without overwriting newer data. |
| REQ-CAT-004 | Must | Authorized users shall deactivate a product without erasing required history. | Deactivation removes the product from default active results, preserves history and emits audit/integration events. |
| REQ-CAT-005 | Must | Authorized users shall manage categories and their active state. | Valid category changes are persisted; a category in prohibited use cannot be removed or deactivated without an explicit policy-compliant outcome. |
| REQ-CAT-006 | Must | Product SKU shall be normalized and unique. | Equivalent normalized SKUs cannot coexist; a duplicate returns conflict and does not change data. |
| REQ-CAT-007 | Must | Product price and stock shall follow defined numeric rules. | Negative price or stock is rejected; decimal precision and currency are deterministic; valid boundary values persist exactly. |
| REQ-CAT-008 | Must | Users shall search and filter the catalog. | Supported combinations of query, category and active state return only matching authorized records with the applied criteria represented in the response. |
| REQ-CAT-009 | Must | Product collections shall be paginated and deterministically sorted. | Page size is bounded, invalid parameters are rejected and repeated requests over unchanged data return stable ordering. |
| REQ-CAT-010 | Must | Material product changes shall create a product history record. | Each successful create/update/deactivate action records actor, time, product, action and a safe before/after change representation. |
| REQ-CAT-011 | Should | Users shall view product history if authorized. | Results are chronological, paginated and do not expose redacted security-sensitive values. |
| REQ-CAT-012 | Must | Catalog writes shall publish an internal domain event after successful commit. | Each material committed change produces one logically identifiable event for downstream handling; rolled-back changes do not publish a committed event. |

### Media (`MED`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-MED-001 | Must | Authorized users shall upload supported product images. | Type, signature, size and authorization are validated before durable acceptance; rejected files are not made publicly retrievable. |
| REQ-MED-002 | Must | Uploaded media shall have an explicit processing lifecycle. | A media item transitions through documented states such as pending, processing, ready or failed, with reason codes for failure. |
| REQ-MED-003 | Must | Image processing shall create only approved variants and metadata. | Successful processing records dimensions, type, checksum and storage references; binary content does not enter transactional tables. |
| REQ-MED-004 | Must | Media retrieval shall enforce product visibility and safe content delivery. | Unauthorized access is rejected; responses use safe content types and do not expose internal storage credentials or paths. |
| REQ-MED-005 | Must | Users shall associate, order and remove product image references. | Association changes are atomic, preserve audit/history needs and do not leave unintended public orphan objects. |
| REQ-MED-006 | Should | Failed media processing shall support bounded retry or authorized reprocessing. | Retry is idempotent, attempt count is visible and terminal failure does not block unrelated product reads. |

### External integration (`INT`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-INT-001 | Must | Material catalog changes shall be synchronized asynchronously to the External Sales Center Mock. | A committed eligible change creates a durable integration task and the user-facing catalog transaction does not depend on downstream availability. |
| REQ-INT-002 | Must | Outbound messages shall carry a unique event ID, schema version, occurred time and correlation context. | Consumers can distinguish event identity/version and trace it to the initiating operation. |
| REQ-INT-003 | Must | Consumers shall process duplicate deliveries idempotently. | Re-delivery of the same event does not duplicate the external business effect and produces an observable duplicate outcome. |
| REQ-INT-004 | Must | Transient failures shall use bounded retry with backoff. | Eligible failures retry according to policy; attempts and next retry are observable; permanent failures are not retried indefinitely. |
| REQ-INT-005 | Must | Exhausted or non-retryable messages shall enter a recoverable failure state. | The message and sanitized failure context are retained for authorized inspection and replay after remediation. |
| REQ-INT-006 | Must | Event publication shall not lose committed catalog changes. | Catalog atomically stores product state, product history and its owned outbox intent; Integration claims/publishes that intent only through the Catalog publication port, with reconciliation for stuck records. |
| REQ-INT-007 | Should | Authorized operators shall inspect synchronization status by product/event. | Status exposes pending, processing, delivered or failed state, attempts and safe timestamps without secrets. |
| REQ-INT-008 | Should | Authorized operators shall replay eligible failed integration work. | Replay requires reason/actor, preserves the original identity relationship and cannot bypass validation or idempotency. |

### Audit (`AUD`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-AUD-001 | Must | Security- and business-relevant actions shall create immutable audit events. | Actor, action, target, outcome, time and correlation ID are recorded; normal application roles cannot edit an audit event. |
| REQ-AUD-002 | Must | Authorized reviewers shall query audit events using bounded filters and pagination. | Results respect least privilege, deterministic order and retention/redaction policy. |
| REQ-AUD-003 | Must | Audit failures shall be visible and shall fail closed for designated critical actions. | A critical action with unavailable durable audit acceptance is rejected; non-critical projections retry and alert. Emergency containment such as Fault Lab stop proceeds even if audit is degraded and raises a critical audit alert. |

### Quality Control Center (`QLT`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-QLT-001 | Must | Authorized users shall create a release and associate immutable release candidates/build identities. | A release version is unique; each candidate references one immutable build identity and evidence cutoff, and a final decision identifies the exact candidate without overwriting previous candidates. |
| REQ-QLT-002 | Must | The system shall register test suites and test cases with stable external references. | Cases include layer, owner, status, automation state and requirement links; duplicate external IDs are rejected. |
| REQ-QLT-003 | Must | The system shall ingest test runs and individual results from approved sources. | Valid, authenticated payloads are idempotently accepted; malformed or unknown-schema payloads are rejected with actionable errors. |
| REQ-QLT-004 | Must | Test results shall preserve execution context and evidence references. | Result status, duration, environment, build/commit identity and evidence metadata are retained without storing secrets. |
| REQ-QLT-005 | Must | Requirements shall be traceable to cases, results, evidence, defects and releases. | A reviewer can navigate all available links in both directions and identify missing links. |
| REQ-QLT-006 | Must | The system shall record defects and associate them with affected requirements, results and releases. | Severity, priority, status, owner/reference and history are captured; closure does not erase prior associations. |
| REQ-QLT-007 | Must | The system shall ingest normalized security findings. | Source identity, rule, severity, affected component, state and evidence reference are retained; duplicates follow a documented fingerprint policy. |
| REQ-QLT-008 | Must | The system shall ingest normalized performance results and thresholds. | Scenario, workload, percentile latency, throughput, error rate and threshold outcome are attributable to a build/release. |
| REQ-QLT-009 | Conditional Must | The Quality Engine shall calculate an explainable Quality Score from versioned inputs and weights. | Each applicable score exposes formula version, contributing metrics, missing-data treatment and calculation time; before Quality Engine maturity it is explicitly `NOT_APPLICABLE`. |
| REQ-QLT-010 | Conditional Must | The Quality Engine shall classify candidate risk using versioned policy. | The applicable level and contributing conditions are shown; identical inputs/policy produce the same result; before Quality Engine maturity it is explicitly `NOT_APPLICABLE`. |
| REQ-QLT-011 | Must | The system shall evaluate versioned quality gates independently of numerical score. | Each required gate has an attributable outcome and reasons; any active critical blocking rule yields a blocking gate outcome and prevents approval; when recommendation capability applies, it forces `BLOCK` even when a calculated score is high. |
| REQ-QLT-012 | Must | The system shall present an evidence summary for an exact release candidate/build. | Authorized users see evidence cutoff/freshness, test status, failures, defects, security findings, performance results, metrics and missing evidence without requiring a score or risk classification. |
| REQ-QLT-013 | Must | An authorized human shall record the final release decision and rationale. | Approval/block/exception records actor, time, rationale, evidence snapshot and any time-bounded exception; a recommendation alone never deploys. |
| REQ-QLT-014 | Should | Quality policies and gates shall be versioned and changes audited. | A release evaluation refers to the exact policy version; policy updates do not silently rewrite historical decisions. |
| REQ-QLT-015 | Conditional Must | The Quality Engine shall produce an explainable release recommendation from gate results, score/risk when applicable and versioned policy. | The recommendation is `APPROVE`, `REVIEW` or `BLOCK`, identifies reasons and policy version, never overrides hard blockers and is `NOT_APPLICABLE` before the Quality Engine exists. |

### Fault Lab (`FLT`)

| ID | Priority | Requirement | Acceptance criteria |
| --- | --- | --- | --- |
| REQ-FLT-001 | Must | Authorized users shall activate only predefined fault scenarios in permitted non-production environments. | Production is denied by design; activation records actor, scenario, scope, expiry and correlation marker. |
| REQ-FLT-002 | Must | Initial scenarios shall cover downstream unavailability/timeout, API latency/HTTP 500, database latency, image failure and queue disruption. | Each implemented scenario has deterministic controls, blast-radius limits and an observable activation state. |
| REQ-FLT-003 | Must | Faults shall expire automatically and support an emergency stop. | A fault cannot persist beyond its maximum TTL; stop is idempotent and its outcome is observable/audited. |
| REQ-FLT-004 | Should | Fault experiments shall link hypothesis, telemetry, test execution and conclusion. | A completed experiment identifies expected behavior, observed signals and unresolved findings. |

## Non-functional requirements

Targets below are initial design goals. Exact thresholds must be baselined and versioned before becoming release gates.

| ID | Quality | Requirement / measurable acceptance |
| --- | --- | --- |
| NFR-PERF-001 | Performance | For an agreed reference dataset and local/CI profile, catalog read endpoints should meet p95 <= 300 ms and write endpoints p95 <= 500 ms, excluding asynchronous downstream completion. |
| NFR-PERF-002 | Performance | Default collection page size shall be 20 and the enforced maximum 100 unless an endpoint documents a stricter value. |
| NFR-REL-001 | Reliability | Committed integration work shall be recoverable across process restart; no acknowledged message may be silently discarded. |
| NFR-REL-002 | Reliability | Retry policies shall be bounded, use backoff with jitter where appropriate and expose terminal failure. |
| NFR-SEC-001 | Security | All protected operations shall enforce server-side authentication and authorization; UI visibility is never an authorization control. |
| NFR-SEC-002 | Security | Secrets, raw credentials, session tokens and sensitive personal data shall not appear in logs, traces, evidence or error responses. |
| NFR-SEC-003 | Security | Dependencies and images shall be scanned under versioned policy; unresolved critical exploitable findings block release. |
| NFR-OBS-001 | Observability | Inbound requests, asynchronous messages and integration attempts shall propagate or create correlation and trace context. |
| NFR-OBS-002 | Observability | Critical flows shall expose structured logs, metrics and traces sufficient to locate the failing boundary without enabling sensitive-data leakage. |
| NFR-OBS-003 | Observability | A test execution that triggers multiple requests shall preserve a bounded set of correlation IDs and associated trace IDs in evidence so each observed failure can be linked to the exact build and execution. |
| NFR-TEST-001 | Testability | External boundaries, time, retry and fault behavior shall be controllable through safe interfaces/fakes in automated tests. |
| NFR-TEST-002 | Testability | Each applicable Must or Conditional Must requirement shall have documented verification coverage before its target release is approved. |
| NFR-DATA-001 | Data integrity | Transactional invariants shall be enforced at appropriate application and database layers; concurrent updates must not silently lose data. |
| NFR-DATA-002 | Data quality | Evidence ingestion shall validate schema, source, timestamps and release/build identity and shall make missing or stale evidence visible. |
| NFR-ACC-001 | Accessibility | User interfaces shall target WCAG 2.2 AA for supported critical flows, including keyboard access, visible focus, names/roles and contrast. |
| NFR-COMP-001 | Compatibility | APIs and events shall use explicit versions and documented compatibility rules; breaking changes require migration planning. |
| NFR-MAINT-001 | Maintainability | Module dependencies shall follow [ARCHITECTURE.md](ARCHITECTURE.md); new cross-module coupling or infrastructure requires review and, when significant, an ADR. |
| NFR-PORT-001 | Portability | Once executable components exist, a contributor shall be able to start required local dependencies through a documented, reproducible workflow. |
| NFR-PRIV-001 | Privacy | Personal and diagnostic data collection shall be minimized and governed by documented retention and deletion policies before production use. |

## Business rules

| ID | Rule |
| --- | --- |
| BR-AUTH-001 | Deny by default: absence of an explicit permission means the action is forbidden. |
| BR-AUTH-002 | Deactivated users cannot initiate new sessions or use revoked sessions. |
| BR-AUTH-003 | Before Phase 03 provides real authentication/RBAC, catalog mutation endpoints are a local-development preview only, must not be externally exposed and cannot satisfy authorization acceptance criteria. |
| BR-CAT-001 | SKU comparison uses one documented normalization rule and is unique across active and inactive products unless an ADR changes reuse policy. |
| BR-CAT-002 | Price is non-negative, uses an explicit ISO 4217 currency and fixed decimal semantics; floating-point arithmetic is forbidden for persisted money. |
| BR-CAT-003 | Stock is an integer greater than or equal to zero; any future reservation model requires separate rules. |
| BR-CAT-004 | Product deletion is logical for the initial scope so required history, audit and release evidence remain referentially meaningful. |
| BR-MED-001 | File extension or client-supplied MIME type alone is never sufficient validation. |
| BR-INT-001 | At-least-once delivery is assumed; consumers must be idempotent. Exactly-once claims are not made across system boundaries. |
| BR-INT-002 | Retry is permitted only for classified transient failures; validation and authorization failures are not made successful through retry. |
| BR-QLT-001 | A Quality Score never cancels or reduces an active hard blocking rule. |
| BR-QLT-002 | Critical security findings, critical test failures and performance error rate beyond the approved critical threshold block release. |
| BR-QLT-003 | Missing required evidence is not a pass; policy determines whether it yields insufficient evidence or a block. |
| BR-QLT-004 | Only an authorized human records the final release decision; automation produces a recommendation. |
| BR-QLT-005 | Exceptions are explicit, justified, time-bounded, attributable and cannot silently alter historical evidence. |
| BR-QLT-006 | When a calibrated policy version defines a block threshold, a Quality Score below it produces `BLOCK`; no numerical threshold is fixed during foundation, and a high score never cancels a hard blocker. |
| BR-FLT-001 | Fault injection is disabled in production and defaults to off everywhere. |
| BR-TEST-001 | A failing test is investigated; it is never deleted, skipped or weakened solely to obtain a green pipeline. |
| BR-AUD-001 | Durable audit acceptance is fail-closed for privileged user/role changes, quality policy/gate changes, final release decisions/exceptions, terminal-message replay and Fault Lab activation/change. Emergency stop and other containment actions must proceed during audit degradation and raise a critical alert. |

## Cross-domain acceptance criteria

A capability is acceptable only when all applicable conditions hold:

1. The intended behavior and boundaries trace to one or more stable requirements.
2. Authentication, authorization, input validation and abuse cases have been considered.
3. State changes are atomic or expose a deliberate, recoverable intermediate state.
4. Relevant business and security actions are auditable.
5. Failures use stable, safe error contracts and do not expose secrets or internals.
6. Tests exist at the lowest effective layers plus scenario coverage where cross-boundary confidence is needed.
7. Required logs, metrics, traces and correlation context support diagnosis.
8. API/event/data compatibility and migration effects have been evaluated.
9. Documentation and traceability links reflect implemented behavior.
10. Applicable quality gates pass, or an authorized exception is recorded under policy. `NOT_APPLICABLE` requires a reason, authorized actor, exact policy version and audit record.

## Open requirement questions

- Which identity mechanism and token/session model best fit local demonstration and future deployment?
- Can a SKU ever be reused after product deactivation, and what downstream consequences would that have?
- Which currencies and locale rules belong in v1.0?
- What evidence storage duration and personal-data retention are required?
- What reference dataset and hardware profile will make performance thresholds reproducible?
- Which source systems are authoritative for defects and requirements in the portfolio version?
- What roles may approve a release exception, and which hard blocks are never overridable?
