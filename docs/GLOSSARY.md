# AEGIS Glossary

This glossary defines cross-document terms. Physical storage and API shapes may evolve, but implementations must preserve these distinctions.

## Release

A logical, versioned delivery scope such as `v1.0.0`. It groups one or more candidates and the historical decisions made about them. A release version is not sufficient to identify executable bits.

## Candidate

One evaluation attempt for a release, bound to exactly one immutable build identity, target environment and evidence cutoff. Multiple candidates may exist for the same release; superseding one never overwrites its evidence or decision history.

## Build

An immutable output identity derived from a specific source commit and resolved build inputs. It may identify one or more artifacts. A display version without commit/artifact identity is not adequate evidence provenance.

## Execution

One attributable invocation of a test suite, test case, security scan, performance scenario, resilience experiment or other approved evidence source. It records effective source, build/candidate, environment, timing, tool/schema version and attempt history.

## Evidence

An attributable result or immutable reference supporting an engineering/release conclusion, such as a test result, report, screenshot, trace, log excerpt, security finding or performance measurement. Evidence includes provenance, freshness, sensitivity and retention metadata; a screenshot or untrusted payload alone is not proof.

## Gate

A versioned deterministic policy condition evaluated against a candidate evidence snapshot. Its outcome is `PASS`, `FAIL`, `INSUFFICIENT_EVIDENCE`, `NOT_APPLICABLE` or `ERROR`. Hard gates cannot be overridden by a numerical score.

## Risk

A versioned classification of candidate uncertainty and potential impact using gate results, evidence and, when applicable, Quality Score. Initial levels are `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` and `UNKNOWN`. Risk is not the same as pass rate.

## Decision

An immutable human-owned record that approves, blocks or—only when policy allows—approves with an explicit time-bounded exception for one exact candidate/build. A machine recommendation is an input to a decision, never the decision itself.

## Related terms

- **Quality Score:** a future explainable derived policy input; `NOT_APPLICABLE` before Quality Engine maturity.
- **Recommendation:** future Engine output (`APPROVE`, `REVIEW` or `BLOCK`) under a versioned policy; it never deploys or cancels a hard blocker.
- **Correlation ID:** a bounded diagnostic identifier for a logical flow; it is not authentication, idempotency or proof of correctness.
- **Trace ID:** an identifier for one causal telemetry trace; one correlation may map to multiple traces after retries or replays.
