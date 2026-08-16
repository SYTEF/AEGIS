# AEGIS Quality Gates and Release Intelligence

## Purpose

Quality gates convert evidence and risk policy into timely decisions. They are progressive: fast feedback runs early and broader validation runs as a change approaches release. Gates protect the product; they are not targets to game.

This is an initial policy proposal. Thresholds and the Quality Score require calibration with real baselines before enforcement. Gate definitions, formulas and exception rules must be versioned so a historical release decision remains explainable.

## Fundamental rule

**Quality Score is a derived policy input. Hard blocking rules are authoritative.**

A calibrated policy may use a low score to produce a `BLOCK` recommendation, but no score is itself evidence and no high score can cancel a hard block. A candidate with a score of 99/100 is still blocked by one active critical security finding, critical test failure or other non-overridable condition. Unknown, stale or missing required evidence is never treated as passing.

## Gate outcome model

Every gate produces one of:

- `PASS`: required conditions were evaluated and met;
- `FAIL`: evaluated conditions did not meet policy;
- `INSUFFICIENT_EVIDENCE`: required input is missing, stale, incompatible or cannot be attributed to the release;
- `NOT_APPLICABLE`: policy explicitly excludes the gate/capability for this candidate and records rationale, authorized actor, exact policy version and audit reference;
- `ERROR`: the gate could not evaluate because of a system/tool failure.

`NOT_APPLICABLE` is not chosen by omission or by an evidence-producing client. It is available only through an authorized, audited policy evaluation. `ERROR` and `INSUFFICIENT_EVIDENCE` are distinct from product failure, but block when the policy requires that evidence.

## Progressive maturity

Quality capabilities become applicable only when their prerequisites exist. Inapplicability never means approval.

| Maturity | Evidence summary | Hard gate evaluation | Quality Score | Risk classification | Engine recommendation |
| --- | --- | --- | --- | --- | --- |
| Foundation / Phase 01 | Documentation/build evidence only | Applicable gates run | `NOT_APPLICABLE` | `NOT_APPLICABLE` | `NOT_APPLICABLE` |
| Catalog through evidence foundation (v0.2–v0.6) | Required for the exact candidate/build as each source exists | Applicable and independent of score | `NOT_APPLICABLE` | `NOT_APPLICABLE` | `NOT_APPLICABLE` |
| Quality Engine maturity (v0.7+) | Required and current | Required first | Calculated only under a validated formula version | Calculated only under a validated risk policy | `APPROVE`, `REVIEW` or `BLOCK` under a versioned policy |

Before v0.7, release review uses current evidence, hard gates, explicit residual risk and a human decision. It must not synthesize a score/risk or infer approval from `NOT_APPLICABLE` values.

Before the Quality Control Center exists, `NOT_APPLICABLE` in this table is a documented maturity rule rather than a fabricated runtime record. Once a summary API persists applicability, it must carry the authorized policy actor/version and audit reference.

## Progressive gates

### 1. Pull Request Gate

Goal: prevent known defects and unsafe changes from entering the default branch while preserving fast feedback.

Required when applicable:

- reviewed change with requirement/issue and risk context;
- compilation/type checking;
- formatting and lint/static analysis;
- changed-scope unit and component tests;
- contract/schema compatibility checks;
- secret scan and high-signal source/dependency checks;
- migration validation when schema changes exist;
- documentation and observability/security impact review;
- no unexplained skipped/weakened tests or reduced critical assertions.

Target feedback: fast checks should normally complete within 10 minutes once a baseline exists; slower justified suites run in parallel or at later gates without removing essential PR protection.

Hard failures: build/type failure, relevant deterministic test failure, detected committed secret, incompatible contract without approved migration, unauthorized weakening of controls/tests.

### 2. Build Gate

Goal: prove a reproducible, identifiable candidate was built from reviewed source.

Required:

- clean build from the selected commit;
- artifact identity and source commit recorded;
- dependency lock/resolution is reproducible;
- artifact/container scanning when those artifacts exist;
- required configuration/schema checks pass;
- software bill of materials/provenance maturity added by the release phase.

An artifact rebuilt from different source/dependency resolution is different evidence even if it uses the same display version.

### 3. Testing Gate

Goal: evaluate functional correctness and data/integration confidence for the candidate.

Required according to change and release scope:

- unit, component and integration suites pass;
- API and contract suites pass;
- selected critical E2E journeys pass when the UI exists;
- data-quality/migration/reconciliation checks pass;
- requirement traceability has no unexplained critical gaps;
- flaky/quarantined tests are reported separately with owner and expiry;
- test results match the exact build and approved environment/profile.

Hard failures: critical test case failure; corruption/lost-update/lost-message behavior; required suite unavailable or attributed to another build; a quarantined critical test with no equivalent evidence.

### 4. Security Gate

Goal: prevent release with known unacceptable exploitable risk.

Required according to exposure:

- secret, source, dependency and artifact scans completed;
- RBAC/object authorization and security regression tests pass;
- threat-model changes reviewed for new boundaries or sensitive capabilities;
- findings triaged by severity, exploitability, reachability and affected release;
- secure upload/API/CI checks applied when relevant.

Hard failures:

- any unresolved confirmed critical exploitable finding affecting the candidate;
- exposed valid secret or credential;
- demonstrated authentication/authorization bypass;
- integrity bypass that can forge quality evidence or release decision;
- Fault Lab reachable in production or by an unauthorized actor.

A scanner label alone requires rapid triage; a confirmed critical issue cannot be averaged away.

### 5. Performance Gate

Goal: ensure critical workloads meet versioned service expectations and do not regress unacceptably.

Required when performance-relevant behavior changes or for release candidates:

- workload, dataset, environment and build identity match the approved profile;
- latency percentiles, throughput and error rate meet scenario thresholds;
- resource saturation, queue age and recovery are reviewed;
- regression against a valid baseline is explained.

Initial provisional API targets are p95 <= 300 ms for catalog reads and p95 <= 500 ms for writes under the agreed reference profile. These are not hard gates until a reproducible baseline defines workload and capacity.

Hard failure after calibration: performance error rate exceeds the scenario's critical threshold, a critical SLO threshold is violated, or the system fails to recover from the approved workload. Environment/load-generator invalidity yields `ERROR`, not pass.

### External Exposure Gate

Goal: prevent a development preview from being mistaken for or deployed as a secure shared system.

For v0.2 and any later build where real authentication/RBAC is incomplete, this gate permits only the documented local/isolated development profile. It **fails** any request to expose catalog mutation externally.

The gate may pass for external exposure only after Phase 03 proves:

- real authentication/session behavior and safe administrator bootstrap;
- server-side role, object and state authorization;
- anonymous/denied/deactivated/session-revocation negative tests;
- closure of every pending authorization acceptance criterion from the Catalog preview;
- removal of development identity as a trusted security boundary;
- reviewed environment configuration, threat model and safe secrets handling.

This gate is independent of Quality Score and is a hard blocker for hosted/public exposure.

### 6. Release Gate

Goal: combine all current evidence into an explicit, auditable release decision.

Required:

- exact release scope, commit/build and evidence cutoff are fixed;
- required prior gates are `PASS` or have an authorized permissible exception;
- hard blocking rules evaluate with current evidence;
- Quality Score, risk classification and Engine recommendation are calculated only when their validated policy applies; otherwise each is explicitly `NOT_APPLICABLE` with the authorized policy record;
- unresolved defects/findings and operational readiness are reviewed;
- rollback/recovery and observability readiness exist for the implemented delivery shape;
- final decision is recorded by an authorized human with rationale.

From Quality Engine maturity onward, automation produces `APPROVE`, `REVIEW` or `BLOCK` recommendation. Before then, recommendation is `NOT_APPLICABLE`; applicable hard gates still block and an authorized human still records the final decision. Automation never deploys or records human approval by implication.

## Change-based gate selection

Every PR receives baseline gates. Additional suites are selected from changed modules, contracts, data migrations, risk tags and historical defects. Examples:

| Change | Mandatory additional focus |
| --- | --- |
| Permission/authentication | security review, allow/deny matrix, session/audit tests |
| Product money/stock/SKU | boundary/property, API, persistence/concurrency and data quality |
| API/event schema | compatibility and consumer/provider contract checks |
| Retry/outbox/worker | database/broker integration, idempotency, restart and resilience |
| Upload/processing | hostile-file security, storage integration, resource/resilience checks |
| Score/gate formula | deterministic policy, missing/stale evidence, hard-block override, audit |
| Performance-sensitive query | execution/access-pattern review and approved performance scenario |
| Telemetry/redaction | observability assertions and sensitive-data leak tests |

Change-based selection cannot omit a suite required by release policy; it controls earlier feedback and targeted regression.

## Initial Quality Score concept

### Goal and boundaries

The score summarizes several quality dimensions for comparison and discussion. It is not a probability of being defect-free, a performance rating for individuals or a substitute for gate details.

Proposed formula shape for future ADR-008 (not yet a policy version):

```text
Quality Score = sum(dimension score x dimension weight) - capped explicit penalties
```

Each dimension is normalized to `0..100`. Initial candidate weights:

| Dimension | Weight | Candidate inputs |
| --- | ---: | --- |
| Functional confidence | 25% | risk-weighted required test outcomes, critical journey results |
| Security confidence | 20% | triaged findings, authorization/security check outcomes |
| Reliability and resilience | 15% | integration, retry/idempotency and approved fault experiments |
| Performance | 15% | threshold outcomes, regression, error rate and recovery |
| Requirement/traceability confidence | 10% | required coverage, missing links, evidence attribution/freshness |
| Data quality | 10% | constraints, migrations, reconciliation and ingestion validity |
| Accessibility | 5% | automated and manual critical-flow outcomes when UI exists |

The formula must define:

- risk weighting rather than raw pass-rate dominance;
- treatment of `skipped`, `blocked`, `error`, retry and quarantine;
- required versus optional/not-applicable dimensions per release maturity;
- freshness window and missing-evidence behavior;
- penalty caps and avoidance of double-counting the same issue;
- precision/rounding;
- minimum sample/profile validity for performance;
- immutable input snapshot and formula version.
- output bounds/clamping and the meaning/cap of any explicit penalty;
- evidence lineage so the same result/finding is not rewarded or penalized redundantly across dimensions;
- treatment of non-applicable dimensions without silent reweighting that inflates the result;
- a versioned block threshold after empirical calibration, with no numeric threshold fixed by this foundation.

Before v0.7, score is `NOT_APPLICABLE`; documentation examples may discuss an experimental formula, but a product UI must not present a synthetic score as release evidence. It becomes a policy input only after validation against representative scenario data and review for misleading outcomes.

## Pass rate rules

Raw pass rate is shown but not used alone:

```text
pass rate = passed results / executed results eligible under the policy
```

The denominator and treatment of skipped/blocked/error are disclosed. Retries retain first-attempt status; eventual pass is not silently merged into a clean pass. Risk-weighted critical failures have higher decision importance than numerous low-risk passes.

## Risk classification

Risk is classified after hard gates and evidence validity are evaluated:

| Level | Meaning | Typical recommendation |
| --- | --- | --- |
| LOW | Required evidence is current, no hard blocks, score/signals are healthy and residual risks are accepted | `APPROVE` |
| MEDIUM | No hard block, but a material non-critical regression, exception, limited gap or uncertainty needs explicit review | `REVIEW` |
| HIGH | Significant unresolved failures/findings, broad uncertainty, weak recovery or poor score indicates likely material impact | `BLOCK` or exceptional senior review if policy permits |
| CRITICAL | At least one non-overridable hard block or demonstrated severe integrity/security risk | `BLOCK` |
| UNKNOWN | Required evidence is missing, stale, erroneous or not attributable | `BLOCK` until evidence policy is satisfied |

Classification policy uses maximum-risk escalation: a strong dimension does not reduce a critical condition in another. The result includes reasons and policy version.

When the Quality Engine applies, its policy version may define:

```text
IF quality_score < versioned_block_threshold
  THEN recommendation = BLOCK
```

No numeric threshold is accepted during foundation. It must be calibrated and versioned. Meeting or exceeding it never changes a failed hard gate.

## Hard blocking rules

Initial mandatory rules:

```text
IF confirmed_critical_security_findings > 0
  THEN BLOCK RELEASE

IF critical_test_failures > 0
  THEN BLOCK RELEASE

IF authentication_or_authorization_bypass = true
  THEN BLOCK RELEASE

IF exposed_valid_secret = true
  THEN BLOCK RELEASE

IF data_integrity_loss_or_corruption = true
  THEN BLOCK RELEASE

IF required_evidence_is_missing_or_stale = true
  THEN BLOCK RELEASE AS UNKNOWN/INSUFFICIENT_EVIDENCE

IF performance_error_rate > versioned_critical_threshold
  THEN BLOCK RELEASE

IF quality_policy_or_evidence_integrity_is_compromised = true
  THEN BLOCK RELEASE

IF fault_lab_production_exposure = true
  THEN BLOCK RELEASE

IF external_exposure_requested = true
  AND real_authentication_rbac_gate != PASS
  THEN BLOCK EXTERNAL EXPOSURE

IF quality_engine_is_applicable = true
  AND quality_score < versioned_block_threshold
  THEN recommendation = BLOCK
```

The low-score rule affects the versioned recommendation policy; it is not a replacement for, or exception to, any preceding hard blocking rule.

Additional release-class-specific rules may be added through versioned policy. A rule cannot be removed merely to make the current candidate pass.

## Exceptions

Some non-critical gate failures may be exceptionally accepted if policy allows. An exception must include:

- affected release, gate, requirement/component and evidence;
- business reason and why remediation cannot precede release;
- severity, likelihood, exposure and customer/operational impact;
- compensating controls and monitoring;
- accountable owner and authorized approver(s);
- expiry/remediation target and verification plan;
- audit record and visibility in the release summary.

Critical security/auth/integrity hard blocks are initially non-overridable. Any proposal to make a critical rule overridable requires a security/architecture ADR and explicit human approval; it cannot be done ad hoc in release data.

Expired exceptions automatically become active risk and cannot approve later releases. Reusing an exception requires a new attributable decision.

Marking a capability or gate `NOT_APPLICABLE` follows the same minimum governance: authorized actor, explicit rationale, exact candidate/build, policy version and audit record. It cannot be used to waive a gate that the policy marks mandatory.

## Flaky and infrastructure failures

- A flaky test is not passed; first-attempt and retry outcomes remain visible.
- Quarantine is tracked, owned and expires under [TEST_STRATEGY.md](TEST_STRATEGY.md#flaky-test-policy).
- Infrastructure/tool failure produces `ERROR`; retry may establish environment validity but cannot erase evidence of instability.
- If required evidence cannot be generated by the cutoff, the release is `UNKNOWN`/blocked rather than optimistically approved.
- Repeated infrastructure instability is a product delivery risk and contributes to risk classification.

## Evidence freshness and provenance

Each gate input must identify:

- source and authenticated ingestion identity;
- exact commit/build/artifact and release;
- environment/profile and tool/schema version;
- execution/measurement time and cutoff;
- result/evidence checksum or immutable reference where appropriate;
- triage state for findings;
- freshness according to policy.

A newer build invalidates evidence for changed scope unless the policy explicitly allows reuse and explains why. Manual evidence is attributable and expires like automated evidence.

## Gate governance

- Gates and formulas are configuration-as-code or equivalently version controlled when implemented.
- Changes receive product, QA, security and engineering review proportional to impact.
- Historical evaluations remain tied to the old version.
- Dashboard exposes inputs, reasons, exceptions and missing data.
- Gate changes are audited and cannot retroactively rewrite a release decision.
- Applicability decisions and policy overrides require dedicated authorization and an immutable audit reference.
- Metrics are periodically tested for perverse incentives and false confidence.

## Initial release summary example

```text
Release: v1.0.0
Candidate: rc.2
Build: <immutable build identity>
Evidence cutoff: <UTC timestamp>

Functional: PASS
API: PASS
Integration: PASS
E2E: PASS
Security: PASS
Performance: PASS

Quality Score: 94/100 (formula 1.0)
Risk: LOW (policy 1.0)
Hard Blocks: none
Recommendation: APPROVE
Final Decision: APPROVED by <authorized actor> at <time>
```

The summary links to individual gates/evidence and shows staleness or exceptions. Presentation text alone is not the evidence.

## Calibration and open questions

- Reference workload/dataset/environment and critical performance error threshold.
- Score normalization and weights validated against representative releases.
- Which requirements and suites are critical for each release class.
- Evidence freshness windows by source.
- Minimum manual accessibility/exploratory evidence for UI releases.
- Exception approver roles and whether any non-security high-risk blocks are non-overridable.
- Definition of coverage metrics that resist superficial test/link inflation.
- Alert/escalation path when a previously approved release later receives a critical finding.
