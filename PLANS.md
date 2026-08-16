# AEGIS Execution Plans

## Plan governance

This file decomposes the [roadmap](docs/ROADMAP.md) into reviewable candidate phases. It is not blanket authorization to implement them. **No phase in this document has been started or implemented.** Each phase requires explicit human approval, scope confirmation and refinement of affected requirements before work begins.

Rules:

- Finish or deliberately replan the current phase before starting the next.
- Keep phases independently reviewable and demonstrable.
- Update requirement IDs, risks and acceptance criteria as learning occurs.
- Do not pull later infrastructure into an earlier phase “for future use.”
- Apply [AGENTS.md](AGENTS.md) and applicable quality/security/observability policies to every phase.
- Create ADRs immediately before important decisions, not as speculative paperwork.

## Phase 01 — Minimal executable backend foundation

### Objective

Prove the smallest reproducible JDK 25 LTS / Spring Boot 4.1.x development path and modular convention without implementing catalog behavior or adding external infrastructure.

### Scope

- Apply the approved technical baseline: JDK 25 LTS, Spring Boot 4.1.x, Maven and executable Jar packaging.
- Use Maven coordinates `io.github.sytef:aegis` and base package `io.github.sytef.aegis`.
- Follow accepted [ADR-001](docs/ADR/ADR-001-modular-monolith.md) for modular-monolith direction and dependency rules.
- Create one backend project with a minimal application entry point.
- Document and expose future liveness, readiness and non-sensitive info behavior using framework-supported mechanisms.
- Adopt `application/problem+json` Problem Details for HTTP errors.
- Establish the bounded `X-Correlation-ID` contract for responses, structured logs and error responses.
- Add the smallest GitHub Actions workflow for Maven build, unit tests and static/basic quality validation with `permissions: contents: read` and no secrets.
- Document local prerequisites and exact run/validation commands.

### Out of Scope

- Frontend, PostgreSQL, RabbitMQ, MinIO, Grafana or Docker Compose.
- Products, categories, users, real authentication, catalog business logic or any CRUD.
- Quality Control Center, Quality Engine, messaging or external mock.
- Full observability stack; only application-level structured diagnostics justified by the skeleton.
- Deployment or public hosting.

### Dependencies

- Explicit human authorization to implement Phase 01; the decisions in this plan authorize planning only.
- Accepted ADR-001 Modular Monolith.
- Availability of JDK 25 LTS and Maven in developer and CI environments.
- Selection of a lightweight modularity enforcement approach consistent with ADR-001; it must not create empty speculative modules.

### Deliverables

- Minimal Maven backend source/build structure producing `aegis` as an executable Jar under group `io.github.sytef` and package `io.github.sytef.aegis`.
- Version/info, liveness/readiness and Problem Details contracts documented in executable OpenAPI or equivalent generated contract where operational endpoints are supported.
- Unit/component smoke tests and architecture-boundary test seed where valuable.
- Basic GitHub Actions validation with `contents: read`, no secrets and no additional permission without explicit justification.
- Local development instructions and version pinning.
- Documentation updates reflecting what now exists versus remains conceptual.

### Acceptance Criteria

1. A fresh contributor can clone, run one documented command and obtain a successful clean build/test without external services.
2. The executable Jar starts; liveness indicates process life only, readiness indicates traffic acceptance and info exposes only service/application/build version and commit SHA when available.
3. Unexpected HTTP errors use `application/problem+json` without stack-trace/configuration leakage.
4. A valid `X-Correlation-ID` of 1–128 allowlisted characters is returned and recorded; missing/invalid input is replaced without echoing unsafe content.
5. The package/module convention prevents or tests at least one forbidden dependency direction without creating empty speculative modules.
6. CI executes Maven build, unit tests and static/basic validation on a clean environment using `permissions: contents: read` and no secrets.
7. No frontend, database, broker, object storage, Grafana, authentication, CRUD or catalog business behavior has been added.

### Expected Tests

- Unit test for any custom correlation/response policy.
- Component test for info/health and safe error behavior.
- Contract tests for liveness/readiness/info disclosure limits and `application/problem+json`.
- Boundary tests for accepted, missing, oversized and invalid `X-Correlation-ID` values.
- Architecture rule test if a reliable lightweight enforcement mechanism is selected.
- Clean build and startup smoke validation.
- Secret/dependency baseline scan appropriate to the created build.

### Risks

- Spending a phase on scaffolding without product value.
- Over-configuring architecture, telemetry or CI before requirements demand it.
- Choosing tool versions/build conventions that raise contributor friction.
- Framework health/info endpoints leaking configuration.
- Toolchain availability or a Spring Boot 4.1.x patch change affecting the pinned build.

Mitigation: constrain this phase to a short vertical technical proof and begin Phase 02 only after review. Reject “future-proof” modules/dependencies.

### Definition of Done

- Acceptance criteria and applicable [AGENTS.md](AGENTS.md#definition-of-done) checks pass.
- Build, static checks and expected tests pass locally and in CI.
- Dependency/security review has no unresolved hard blocker.
- README/API/architecture/local-run documentation matches the executable skeleton.
- Complete diff, CI permissions and generated artifacts are reviewed.
- No database, frontend or unrelated feature is present.
- Workflow permissions are exactly `contents: read` unless a reviewed requirement justifies more.

## Phase 02 — Catalog core and persistence local preview (v0.2)

### Objective

Deliver the first real product-value slice through an API backed by PostgreSQL as a **LOCAL DEVELOPMENT PREVIEW**, without claiming secure mutation until Phase 03 closes authentication/RBAC.

### Scope

- Refine `REQ-CAT-001` through `REQ-CAT-012` into implementation-ready examples, selecting a small sequence of vertical slices.
- Decide PostgreSQL version, migration tool and schema ownership through ADR-002.
- Decide concurrency contract (ETag or explicit version) through ADR-007.
- Implement category and product create/read/update/deactivate, SKU/money/stock invariants, pagination/filter/sort and product history.
- Persist Catalog-owned domain-event intent atomically with product state/history; no broker or Integration publisher is introduced yet.
- Introduce minimum audit append behavior for catalog changes without building the full audit UI.
- Add database migrations, deterministic local data and reproducible local PostgreSQL dependency.
- Publish executable OpenAPI for implemented endpoints.
- Add proportionate structured logs, metrics/health detail and correlation for catalog/database paths.

### Out of Scope

- Real authentication/RBAC. A clearly labeled development identity may support local testing but is not authentication evidence and cannot satisfy authorization acceptance criteria.
- Product images, messaging, external integration and QCC.
- Browser frontend.
- Hierarchical categories, multi-currency expansion, cart/order/payment.
- Production deployment/high availability.
- Public/external exposure, shared demo hosting or any environment reachable beyond the approved local/isolated development boundary.

### Dependencies

- Phase 01 completed.
- Approved ADR-002 and ADR-007.
- Resolved category cardinality, SKU reuse and initial currency questions.
- Docker or an ADR-approved reproducible local PostgreSQL alternative.
- An enforceable External Exposure Gate that fails when this pre-authentication profile is configured for public/shared hosting.

### Deliverables

- Catalog module/domain, public application/API boundary and PostgreSQL adapter.
- Versioned migrations and local seed/factory mechanism.
- Implemented OpenAPI/error/pagination/concurrency contracts.
- Product history and safe audit facts.
- Catalog unit, component, database integration and API suites.
- Local run/reset documentation and CI database test profile.
- Persistent local-preview warning and configuration guard showing that catalog mutation is not authorization-complete.

### Acceptance Criteria

1. Valid product/category workflows satisfy the approved `REQ-CAT-*` subset and invalid writes are atomic.
2. Normalized duplicate SKU, negative price/stock, invalid category and stale update are rejected deterministically.
3. Repeated pagination over unchanged data is stable and maximum page size is enforced.
4. Material changes create accurate actor/time/correlation-aware history.
5. Database constraints protect critical invariants and migrations work from an empty database.
6. API errors are safe and contract-compatible; no direct persistence entity is exposed.
7. Local and CI setup is reproducible and documented.
8. External Exposure Gate proves the pre-authentication catalog cannot be configured/deployed as an externally reachable environment.
9. Authorization portions of `REQ-CAT-*` remain explicitly pending; no test using a development identity is reported as RBAC evidence.

### Expected Tests

- Unit/property/parameterized tests for SKU, money, stock, transitions and mappings.
- Component/API happy, boundary, validation, pagination, concurrency and error tests.
- PostgreSQL integration tests for uniqueness, transactions, constraints and migrations.
- Data-quality checks for history and rollback/no partial state.
- Basic performance smoke for pagination/query regressions, not yet a hard load gate.
- Security tests for mass assignment, injection input and error disclosure even before full auth.
- Configuration/architecture test for the External Exposure Gate; authorization tests remain pending until Phase 03.

### Risks

- A temporary unauthenticated API could be mistaken for release-ready behavior.
- Data model inflation beyond current access patterns.
- Currency/category ambiguity causing incompatible schema/API decisions.
- Test containers/local Docker friction.

### Definition of Done

- Approved catalog requirements and contract examples trace to passing tests.
- Build, lint/static analysis, unit/component/integration/API/migration checks pass.
- No lost update or partial-write defect in covered scenarios.
- Threat and telemetry reviews are updated for database/API behavior.
- OpenAPI, data model, architecture and local-run docs match implementation.
- v0.2 release evidence and limitations are recorded; no claim of secure multi-user use.
- Documentation and UI/API diagnostics label v0.2 as local preview; Phase 03 is a mandatory security closure before exposure.

## Phase 03 — Authentication, RBAC and Commerce UI (v0.3)

### Objective

Protect catalog operations with real identity/RBAC and expose a minimal accessible Commerce workflow in React/TypeScript.

### Scope

- Accept ADR-006 for authentication/session architecture.
- Implement approved `REQ-AUTH-*`, user/role bootstrap and catalog permission matrix.
- Close every authorization acceptance criterion left pending by the Phase 02 local preview and replace the development identity boundary.
- Enforce endpoint and object/state authorization; audit security-relevant actions.
- Create a minimal frontend shell with login/session handling and core product/category list/edit flows.
- Apply secure browser configuration, CSRF/CORS/token storage policy and accessibility fundamentals.
- Add frontend build/lint/unit/component strategy and limited Playwright critical journeys.

### Out of Scope

- Public registration, social login, complex identity federation or MFA unless ADR explicitly includes them.
- Media, messaging and QCC.
- Pixel-perfect design system or broad E2E permutation coverage.
- Production identity recovery/compliance claims.

### Dependencies

- Phase 02 completed and stable API contracts.
- Role/permission matrix, administrator bootstrap and deactivation/session requirements approved.
- Authentication ADR and frontend package/tooling decision.
- Defined supported browser baseline.

### Deliverables

- Auth module, protected catalog APIs and migration changes.
- Minimal React/TypeScript Commerce experience with accessible forms/navigation.
- Security audit events and authentication/authorization telemetry.
- Updated OpenAPI/threat model and local demo identities using inert synthetic data.
- Unit/component/API/security/accessibility/Playwright coverage.

### Acceptance Criteria

1. Active users authenticate/logout according to the selected model; invalid/deactivated/revoked sessions fail safely.
2. Permission and object/state denials are server-enforced and audited without resource leakage.
3. Catalog UI critical flows are keyboard operable, labeled and expose validation/error states accessibly.
4. Browser session handling meets ADR cookie/token, CSRF, CORS and security-header policy.
5. Admin bootstrap does not ship a known default credential and is reproducible locally.
6. UI absence/hiding of a control is never the only authorization protection.
7. The External Exposure Gate remains blocking until real authentication/RBAC, safe bootstrap and all pending Catalog authorization checks pass; only then may a separately approved hosted environment be considered.

### Expected Tests

- Unit tests for permission/session policies.
- API matrix of anonymous, allowed, denied, deactivated and stale-session cases.
- Role-administration tests for create/disable, assignment/removal, last-admin protection and session revocation after critical permission changes.
- Authentication abuse/rate/generic-error and audit/redaction tests.
- Frontend unit/component tests for forms/state/errors.
- Automated accessibility rules plus manual keyboard/screen-reader smoke.
- Few Playwright journeys for login, allowed catalog change and denied action.

### Risks

- Token storage/CSRF design errors and authorization gaps.
- Single-person role overlap hiding segregation limitations.
- UI test overuse and fragile locators.
- Frontend/backend contract drift.

### Definition of Done

- Auth/RBAC requirements and threat mitigations pass with allow/deny evidence.
- Build/lint/tests run for backend and frontend; critical Playwright journeys are stable without arbitrary sleeps.
- No credential/token leakage in logs, URL, source or artifacts.
- Accessibility manual/automated findings are triaged.
- API/security/architecture/local demo documentation is current.
- The Phase 02 insecure-preview warning is retired only after the External Exposure Gate proves the closure criteria.

## Phase 04 — Secure media lifecycle (v0.4)

### Objective

Support product images through a safe, private and observable processing lifecycle.

### Scope

- Accept ADR-004 for MinIO/object storage and decide upload flow.
- Implement approved `REQ-MED-*`, metadata, states, associations/order and cleanup.
- Validate content signature, type, byte/pixel/decompression limits and authorization.
- Re-encode approved formats in a constrained processor and strip unnecessary metadata.
- Implement retry/reprocessing and visible failure status.
- Add MinIO/local configuration only for this phase.

### Out of Scope

- Video/documents/SVG, public CDN and production malware-analysis service.
- Arbitrary transformations or user-defined processing.
- Media search/asset management beyond product needs.
- RabbitMQ unless processing evidence shows a broker is needed now and an ADR approves it; a simple recoverable execution model is preferred first.

### Dependencies

- Phase 03 authentication/RBAC and product boundaries.
- Approved format/size/dimension/retention rules.
- MinIO ADR and selected safe image library/process isolation approach.

### Deliverables

- Media module and secure API/UI workflow.
- Private object storage profile, metadata migration and reconciliation/cleanup.
- Processing status/retry telemetry and user-visible outcomes.
- Hostile-file, storage integration, resilience and accessibility coverage.
- Updated threat model and operational limits.

### Acceptance Criteria

1. Valid supported images become ready with correct safe variants/checksum/metadata.
2. Extension/MIME mismatch, oversize/decompression risk and unauthorized upload/retrieval are rejected safely.
3. Internal keys/credentials and pending/failed objects are not publicly exposed.
4. Processor/storage failure produces visible recoverable/terminal state without corrupting product data.
5. Delete/reorder/retry are authorized, concurrency-safe and audited.
6. Reconciliation detects intended orphan/stuck cases without destructive ambiguity.

### Expected Tests

- Unit tests for type/state/limit policies.
- API authorization, validation, ordering and status tests.
- MinIO integration for storage/retrieval/cleanup/unavailability.
- Hostile fixtures: mismatched signature, truncated, polyglot candidates and decompression/dimension boundaries using safe inert samples.
- Processor timeout/failure/retry and orphan reconciliation tests.
- UI accessibility and focused Playwright media journey.

### Risks

- Decoder vulnerabilities/resource exhaustion.
- Orphan objects and irreversible cleanup mistakes.
- Signed URL or bucket exposure.
- Introducing messaging prematurely.

### Definition of Done

- Media requirements, threat controls and recovery scenarios pass.
- No unsupported active format or public object default exists.
- Build/static/security/dependency checks and layered tests pass.
- Limits, lifecycle, cleanup, storage and local run documentation are current.
- Diagnostic evidence identifies image stage/failure without file data leakage.

## Phase 05 — Reliable external integration (v0.5)

### Objective

Synchronize committed catalog changes to a controlled External Sales Center Mock without losing changes or duplicating business effects.

### Scope

- Finalize event envelope/schema and accept ADR-003 for RabbitMQ topology/retry/DLQ.
- Use the Catalog-owned transactional outbox intent and publication port; implement the Integration-owned publisher, delivery handling, worker and mock contract without direct Catalog table access.
- Implement idempotency, bounded retry/backoff, timeouts, terminal failure, status, reconciliation and authorized replay.
- Propagate correlation/trace context and expose queue/outbox/delivery health.
- Add RabbitMQ/local mock dependencies only now.

### Out of Scope

- Real sales provider, Kafka, exactly-once claims or microservice extraction.
- Generic integration platform, workflow engine or arbitrary message routing.
- Automatic replay of permanent/unknown failures.

### Dependencies

- Stable catalog commit/history model and auth permissions.
- Event payload minimization/compatibility review.
- RabbitMQ ADR, mock contract and retry/error taxonomy.
- Defined operator permission and replay audit behavior.

### Deliverables

- Versioned catalog event and downstream HTTP contracts.
- Catalog publication-port integration plus publisher/worker/mock implementation; Catalog remains owner/writer of intent and Integration owns publication/delivery.
- Integration status/replay capability and safe telemetry.
- Broker/mock Docker Compose additions and runbook.
- Contract/integration/restart/idempotency/resilience suites.

### Acceptance Criteria

1. A committed eligible catalog change eventually reaches a healthy mock and is traceable end-to-end through the Catalog publication port.
2. Rolled-back changes do not yield committed events; committed changes survive process/broker downtime.
3. Duplicate publish/delivery/replay does not duplicate downstream business effect.
4. Transient failure retries within policy; permanent/exhausted failure is retained and visible.
5. Catalog writes remain available while the downstream is unavailable when outbox capacity is safe.
6. Replay is authorized/audited and preserves identity/idempotency.

### Expected Tests

- Transactional database tests for catalog/history/outbox atomicity.
- Architecture tests proving Integration cannot access Catalog repositories/tables directly.
- Publisher crash-window and claim/restart tests.
- Broker duplicate/redelivery/backlog/unavailability integration tests.
- Provider/consumer contract tests including errors and version incompatibility.
- Worker timeout/retry/backoff/idempotency/DLQ/replay tests.
- Fault experiments for downstream unavailable/slow and recovery.

### Risks

- Dual-write loss, duplicate effect and poison-message loops.
- Retry storms or unbounded queue/resource growth.
- Mock and adapter agreeing on an incorrect undocumented contract.
- Overclaiming service independence/exactly-once semantics.

### Definition of Done

- `REQ-INT-*` acceptance and failure scenarios pass with retained evidence.
- No silent loss or duplicate business effect in tested crash/retry windows.
- Contract/version, topology, runbook and recovery documentation is current.
- Security/least-privilege broker/replay review and telemetry dashboards pass.
- Phase remains a modular-monolith codebase even if worker runs separately.

## Phase 06 — Quality evidence and Control Center (v0.6)

### Objective

Build trustworthy release/test/evidence traceability and an accessible UI using actual AEGIS sources before calculating a Quality Score.

### Scope

- Implement releases/candidates, suites/cases, runs/results, evidence references, defects, security findings and performance results for `REQ-QLT-001` through `REQ-QLT-008`.
- Implement independent versioned hard-gate evaluation (`REQ-QLT-011`) and the complete candidate evidence summary (`REQ-QLT-012`).
- Add scoped, authenticated, schema-versioned, idempotent ingestion APIs.
- Attribute all evidence to exact commit/build/release/environment and show freshness/gaps.
- Implement bidirectional requirement-case-result-evidence-defect-release navigation.
- Create initial Quality Control Center views for release summary and source status.
- Integrate real outputs from existing AEGIS test/security/performance tools as they exist.

### Out of Scope

- Quality Score, risk classification, Engine recommendation and final decision workflow. These are explicitly `NOT_APPLICABLE`, not zero/passing.
- General-purpose test management or integrations with every external tracker.
- Copying all raw CI logs/artifacts into PostgreSQL.
- Fabricated data solely to make dashboards look complete (demo fixtures must be labeled).

### Dependencies

- Existing trustworthy build/test identifiers and reports.
- Approved ingestion schema, batch atomicity/limits, source identity and evidence retention decisions.
- Requirement/test ID conventions and build artifact provenance.
- Accessible UI foundation from Phase 03.

### Deliverables

- Quality module data/API/UI for evidence and traceability.
- Ingestion adapters/contracts for approved sources.
- Evidence access/redaction/retention implementation.
- Source freshness, rejection and duplication telemetry.
- Data-quality, API, security, accessibility and E2E suites.

### Acceptance Criteria

1. Duplicate source run ingestion is idempotent; conflicting/malformed/unsupported input is rejected visibly.
2. A release view distinguishes passed, failed, skipped, blocked, error, retry and missing/stale evidence.
3. A reviewer can follow the required traceability chain and see gaps rather than inferred links.
4. Every result can be attributed to exact build/environment/source and safe evidence.
5. Raw sensitive artifacts require narrower access than aggregate summary.
6. Dashboard aggregations match independently checked source fixtures and never convert unknown to pass.
7. Evidence summary and hard gates work for an exact candidate/build while score, risk and Engine recommendation remain audited `NOT_APPLICABLE` without implying approval.

### Expected Tests

- Unit tests for normalization, aggregation, status and freshness policies.
- API schema/version/auth/size/idempotency/batch behavior tests.
- Database integration and data-quality reconciliation tests.
- Security tests for forged source, cross-release attribution, evidence URL/access and payload abuse.
- Accessibility/manual exploration of release/traceability views.
- Playwright critical traceability journey.

### Risks

- Untrustworthy provenance creating false confidence.
- Storing too much sensitive/high-volume evidence.
- Dashboard aggregation bugs or ambiguous statuses.
- Building a generic test manager instead of AEGIS release needs.

### Definition of Done

- Approved quality evidence requirements pass with known source fidelity.
- Aggregations are independently verified against representative fixtures.
- Security/access/retention and observability controls pass.
- UI critical paths meet accessibility checks.
- Product output returns score, risk and Engine recommendation only as governed `NOT_APPLICABLE`; it does not display a synthetic numerical score.
- The resulting capability set may be evaluated against the MPR checkpoint without claiming the v1.0 Full Vision.

## Phase 07 — Quality Engine and release decisions (v0.7)

### Objective

Produce transparent hard-gate evaluations, experimental score, risk classification and recommendation while preserving human decision authority.

### Scope

- Implement versioned gate policy/evaluations and hard blocking rules.
- Implement `REQ-QLT-009`, `REQ-QLT-010` and `REQ-QLT-015`: calibrate score dimensions, risk policy, recommendation threshold and missing-data behavior.
- Implement versioned risk classification and explainable recommendation.
- Record authorized human final decisions and allowed time-bounded exceptions.
- Display formula/policy version, contributors, blocks, evidence cutoff and rationale.
- Add adversarial scenarios designed to expose misleading aggregation.

### Out of Scope

- Opaque ML/AI release decisions, automatic deployment or score-based bypass of critical rules.
- Retroactive rewriting of historical evaluations/decisions.
- Claiming predictive accuracy without representative validation.

### Dependencies

- Phase 06 trusted/fresh evidence.
- Approved formula/gate ADR-008, critical requirement map, thresholds and exception authority.
- Representative release scenarios for calibration.

### Deliverables

- Versioned deterministic Quality Engine and evaluation records.
- Hard-gate, risk, recommendation and final decision UI/API.
- Calibration/adversarial dataset and evaluation report.
- Audit/telemetry for policy changes/evaluation errors/decisions.
- Updated quality gate governance and operator documentation.

### Acceptance Criteria

1. Same immutable inputs and policy version yield the same score/risk/recommendation.
2. Any critical hard-block scenario recommends `BLOCK` regardless of score.
3. A score below the calibrated `versioned_block_threshold` recommends `BLOCK`; no high score changes a hard-gate failure.
4. Missing/stale/error input produces explicit insufficient/unknown behavior, never pass.
5. Every score exposes contributors/weights/formula and every risk/recommendation exposes reasons.
6. Only authorized humans record final decisions/exceptions with immutable rationale and expiry.
7. Policy updates do not alter historical evaluation results.

### Expected Tests

- Unit/property/decision-table tests for formulas, boundaries, rounding and hard blocks.
- Mutation/adversarial tests that attempt to offset critical failure with high low-risk pass volume.
- Data integration tests for evidence cutoff/version/history.
- API/RBAC/audit tests for policy and decisions.
- UI accessibility and Playwright decision-review journey.
- Calibration review against representative releases and known outcomes.

### Risks

- False precision, perverse incentives and double-counted dimensions.
- Policy tampering or unauthorized exception.
- Stale evidence and score presentation misleading users.
- Calibration data too small to justify weights.

### Definition of Done

- Hard rules and explainability pass adversarial review.
- Formula remains labeled experimental if validation is insufficient.
- Security/audit/access and immutable history checks pass.
- Documentation states limitations/residual assumptions plainly.
- Human approval remains separate from recommendation.

## Phase 08 — Security and performance hardening (v0.8)

### Objective

Establish reproducible, risk-driven security and performance assurance over the implemented product.

### Scope

- Refresh threat model and conduct targeted manual testing of highest-risk surfaces.
- Mature source/dependency/secret/artifact scanning and triage workflow.
- Create k6 reference workload/dataset/environment and baselines.
- Tune/index/fix measured bottlenecks without changing semantics.
- Calibrate performance/security gates and source them into the Quality Control Center.
- Exercise abuse/resource limits for login, search, upload, ingestion and replay.

### Out of Scope

- Formal compliance certification, public penetration-test claims or universal scale promises.
- Tuning without measurement, production load testing without authorization or hiding functional errors as performance noise.
- Kubernetes/auto-scaling as a default performance response.

### Dependencies

- Stable critical product journeys and representative dataset.
- Defined supported environment/hardware profile and workload model.
- Finding severity/exploitability triage policy and responsible owners.

### Deliverables

- Updated threat model/security test report and remediation evidence.
- Version-controlled k6 scenarios, dataset generator and baseline report.
- Calibrated threshold/gate policy with environment validity criteria.
- Security/performance dashboards and QCC ingestion.
- Runbooks for regression investigation.

### Acceptance Criteria

1. Reference workloads are reproducible and identify build/environment/resources.
2. Critical endpoints meet approved percentile/error/recovery targets or risks are blocked.
3. No unresolved confirmed critical exploitable finding exists.
4. Auth/upload/search/ingestion resource/abuse controls fail safely under approved scenarios.
5. Performance changes preserve correctness/data integrity and show measured improvement.
6. Reports distinguish tool/environment invalidity from product outcome.

### Expected Tests

- k6 smoke/baseline/load and selected stress/soak only when justified.
- Manual and automated authentication/RBAC/upload/API/ingestion security tests.
- Static/dependency/secret/artifact scans with triage/retest.
- Regression/integration tests around performance optimizations.
- Resilience/recovery checks during load where safe.

### Risks

- Noisy or nonrepresentative performance environment.
- Scanner false positives/negatives and checkbox security.
- Optimizations that weaken validation, audit or consistency.
- Metrics used as marketing claims beyond evidence.

### Definition of Done

- Reproducible baselines and exact limitations are documented.
- Applicable security/performance hard gates pass with current evidence.
- Optimizations retain functional/data/security regression coverage.
- Remaining findings and thresholds have explicit rationale/owner.
- QCC accurately represents source results and freshness.

## Phase 09 — Full observability and Fault Lab (v0.9)

### Objective

Demonstrate that critical failures can be safely injected, observed, investigated and recovered in non-production environments.

### Scope

- Accept observability backend/topology decision and add OpenTelemetry, Prometheus and Grafana components needed locally.
- Implement critical logs/metrics/traces, dashboards, alerts and runbooks from [OBSERVABILITY.md](docs/OBSERVABILITY.md).
- Implement Fault Lab authorization, environment denial, allowlisted scenarios, TTL and emergency stop.
- Cover downstream unavailable/timeout, API latency/500, database latency, image failure and queue disruption incrementally.
- Link experiment hypothesis, test run, trace/evidence and conclusion into QCC.

### Out of Scope

- Production fault injection, arbitrary scripts/URLs/SQL, chaos platform or 24/7 SRE claims.
- Instrumenting every method or retaining unlimited telemetry.
- Alerting on every error without actionability.

### Dependencies

- Mature critical flows, correlation and existing telemetry fundamentals.
- Approved safe fault controls, maximum durations/intensity and environment identity.
- Observability storage/retention/cost decision and local resource budget.

### Deliverables

- Local telemetry stack/profile, critical instrumentation and versioned dashboards/alerts.
- Fault Lab controls/UI/API with unmistakable active state.
- Experiment catalog, abort/recovery procedures and investigation runbooks.
- Resilience, security, telemetry and recovery evidence in QCC.

### Acceptance Criteria

1. Production configuration independently prevents Fault Lab activation; default is off everywhere.
2. Only authorized allowlisted scenarios run within scope/TTL and emergency stop is idempotent/audited.
3. Each scenario demonstrates expected degraded behavior, bounded blast radius and recovery without silent data/message loss.
4. QA can navigate from failing execution/correlation to the responsible boundary using safe telemetry.
5. Dashboards avoid sensitive/high-cardinality labels and alerts have actionable runbooks.
6. Telemetry outage does not deadlock application work or fabricate successful evidence.

### Expected Tests

- Permission/environment/parameter/TTL/emergency-stop security tests.
- Trace propagation across HTTP/database/outbox/broker/worker/downstream.
- Metric classification/cardinality and log/trace secret-redaction tests.
- Health checks during dependency failures/recovery.
- Fault experiments with hypothesis, abort condition, observed signals and data-integrity reconciliation.
- Alert rule/runbook exercise using controlled scenarios.

### Risks

- Fault escape/persistence and unsafe blast radius.
- Local stack resource complexity reducing reproducibility.
- Telemetry leakage, high cardinality or sampling gaps.
- Tests coupled to exact log wording rather than product behavior.

### Definition of Done

- Safety controls and production denial pass independent review.
- Each implemented scenario has repeatable outcome/recovery evidence.
- QA investigation walkthrough meets expected diagnosis without privileged database guesswork.
- Telemetry security/cardinality/retention and local resource needs are documented.
- No unresolved critical Fault Lab or observability security risk exists.

## Phase 10 — v1.0 Full Vision portfolio release

### Objective

Consolidate AEGIS into the coherent, reproducible and honestly scoped Full Vision portfolio release while preserving the separately demonstrable MPR core.

### Scope

- Close or explicitly rescope v1.0 Must requirements and traceability gaps.
- Polish accessible critical Commerce and QCC journeys.
- Stabilize local bootstrap, deterministic demo, CI gates and artifact identity.
- Execute final functional, integration, security, performance, resilience, accessibility and data-quality evidence plan.
- Review all documentation/ADRs, score/gates/risk and residual limitations.
- Record a human v1.0 release decision.

### Out of Scope

- Pretending local portfolio operation is a public SLA/compliance certification.
- New major capability, infrastructure migration or architectural rewrite.
- Hiding defects, flaky tests, gaps or open risk for presentation.
- Commit/push/deployment without separate explicit authorization.

### Dependencies

- Prior approved milestones completed or explicitly descoped with documentation.
- MPR capability checkpoint demonstrated or its remaining gap explicitly resolved before Full Vision polish.
- Release candidate scope/build frozen and evidence policy ready.
- Demo target decision: reproducible local only versus separately assessed hosted demo.

### Deliverables

- Tagged-ready (but not automatically tagged) v1.0 candidate and immutable evidence inventory.
- Accessible scripted demo and failure-investigation walkthrough.
- Reproducible local setup/teardown and troubleshooting docs.
- Final traceability, security, performance, resilience and observability reports.
- Quality evaluation, risk/recommendation and authorized final decision.
- Portfolio narrative linking engineering choices to evidence and limitations.

### Acceptance Criteria

1. A fresh reviewer can follow documented prerequisites to run the product and core validation in the declared environment.
2. Critical Commerce, QCC, integration and fault-investigation journeys behave as documented.
3. Required gates use current evidence from the exact candidate; no hard blocker remains.
4. Must requirements are implemented and traced or explicitly removed from v1.0 scope with approval.
5. Security/performance/accessibility claims state tested scope/environment and do not overgeneralize.
6. Architecture contains no unjustified prohibited technology or undocumented critical decision.
7. Final decision distinguishes score, risk, recommendation, exceptions and human approval.

### Expected Tests

- Full approved release suite across unit/component/integration/API/contract/E2E.
- Security scanning plus targeted manual high-risk retest.
- Reference k6 scenarios and regression comparison.
- Selected Fault Lab resilience/recovery experiments.
- Accessibility automated/manual critical-flow review.
- Data reconciliation/migration/traceability/evidence-freshness checks.
- Clean-machine/local reproducibility rehearsal.

### Risks

- Demo polish displacing risk remediation.
- Environment drift and stale evidence.
- Flaky/infrastructure failures being hidden near release.
- Claims exceeding actual deployment/test scope.
- Documentation inconsistency after accumulated evolution.

### Definition of Done

- Release Gate passes under current versioned policy with no non-overridable hard block.
- All evidence is attributable, current and reviewed; exceptions are explicit and valid.
- Clean reproducibility and demo/investigation walkthrough succeed.
- Security, performance, accessibility, data and operational limitations are documented.
- Full diff/repository status, documentation links and ADR index are reviewed.
- An authorized human records the release decision; commit, tag, push or deploy happens only under a separate explicit request.

## Recommended next approval

The planning baseline is now fixed: JDK 25 LTS, Spring Boot 4.1.x, Maven, executable Jar, `io.github.sytef:aegis`, base package `io.github.sytef.aegis`, accepted ADR-001 and least-privilege GitHub Actions (`contents: read`).

These decisions do not authorize implementation. Approve **Phase 01 only** through a separate explicit human instruction after Gate 01 documentation passes. Phase 01 must remain small enough for one focused review. If discussion expands into catalog/database/frontend work, split it rather than silently enlarging the phase.
