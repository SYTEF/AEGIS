# AEGIS Conceptual API Specification

## Status

This is a design-level contract, not executable OpenAPI and not an implemented API. Endpoint details may be refined during their roadmap phase, but changes must remain aligned with [requirements](REQUIREMENTS.md), security policy and compatibility rules. An executable OpenAPI document should become the contract source when backend implementation starts.

## Conventions

- Base path: `/api/v1`.
- Format: JSON unless an endpoint explicitly handles binary/multipart content.
- Transport: HTTPS outside local development.
- Authentication: bearer/session mechanism to be selected by ADR; protected endpoints always enforce server-side authorization.
- Content type: successful JSON responses use `application/json`; error responses use `application/problem+json`; upload uses controlled `multipart/form-data` or a signed-upload design selected later.
- Timestamps: ISO 8601 UTC instants, for example `2026-08-16T15:00:00Z`.
- IDs: opaque strings; clients must not infer ordering or type from them.
- Money: `{ "amount": "19.90", "currency": "BRL" }`; decimal values are serialized as strings to preserve precision.
- Unknown request fields: rejected for security-sensitive commands unless an explicit compatibility policy allows them.
- Correlation: accept a valid `X-Correlation-ID` or generate one; always return the effective ID in the response header and diagnostic context.
- Versioning: breaking API changes require a new major path/media version and migration notice; additive optional fields are normally compatible.

### Phase 01 operational contracts

These contracts are approved for future Phase 01 implementation; they are not implemented by this documentation change. Operational endpoints sit outside the `/api/v1` business path.

| Endpoint | Purpose | Contract |
| --- | --- | --- |
| `GET /actuator/health/liveness` | Indicate only whether the process is alive | `200` with minimal `UP`; it must not call external dependencies or expose configuration |
| `GET /actuator/health/readiness` | Indicate whether the application can safely accept traffic | `200` when ready, `503` when not ready; dependency rules evolve only when dependencies exist |
| `GET /actuator/info` | Return non-sensitive build metadata | May expose service name, application version, build version and commit SHA when available |

Info/health responses must never expose secrets, environment-variable dumps, credentials, internal paths, raw configuration, dependency URLs or stack traces. Phase 01 has no database, broker, object store or downstream service, so readiness must not invent dependency checks.

### Correlation ID contract

- Header name: `X-Correlation-ID`.
- A received value is accepted only when its length is `1..128` characters and every character matches the allowlist `[A-Za-z0-9._-]`.
- An absent, blank, oversized or invalid value is replaced with a server-generated opaque identifier; the rejected value is never echoed or logged verbatim.
- The effective ID appears in the response header, structured logs and Problem Details error body.
- A correlation ID is diagnostic only: it is not authentication, authorization, idempotency or uniqueness proof, and clients may legitimately reuse one for a logical flow.
- Downstream propagation must preserve the bounded canonical value and prevent header/log injection.

## Authentication and authorization

| Access | Meaning |
| --- | --- |
| Public | No user session required, but rate limits and input controls still apply |
| Authenticated | Valid active identity required |
| Permission | Active identity plus named server-side capability, such as `catalog:write` |

Authorization failures use `401` when identity is missing/invalid and `403` when a known identity lacks permission. Responses must not reveal whether inaccessible protected resources exist.

Initial permission vocabulary (subject to authentication ADR):

- `catalog:read`, `catalog:write`, `catalog:history:read`;
- `media:read`, `media:write`;
- `quality:read`, `quality:write`, `quality:ingest`, `quality:policy:admin`, `release:decide`;
- `audit:read`, `faultlab:operate`, `admin:users`, `admin:roles`.

The minimum roles `ADMIN`, `QUALITY_MANAGER`, `OPERATOR` and `VIEWER` are defined in [REQUIREMENTS.md](REQUIREMENTS.md#minimum-conceptual-role-matrix). `quality:policy:admin` is a distinct permission and is never implied by `quality:write`.

## Common response contracts

### Resource metadata

Mutable resources expose `id`, `createdAt`, `updatedAt` and `version` where relevant. Update commands supply the expected version through `If-Match` or a request field; the implementation phase must choose one consistent pattern.

### Collection response

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "sort": ["name,asc"]
}
```

Page indices start at `0`. Default size is `20`, maximum `100`, and sort must include a stable tie-breaker. Invalid or unbounded parameters return `400`.

### Error response

```json
{
  "type": "https://aegis.local/problems/validation-error",
  "title": "Request validation failed",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "One or more fields are invalid.",
  "correlationId": "01J...",
  "timestamp": "2026-08-16T15:00:00Z",
  "errors": [
    { "field": "sku", "code": "REQUIRED", "message": "SKU is required." }
  ]
}
```

The response media type is `application/problem+json`. The shape follows Problem Details semantics while adding a stable application `code`. `detail` and field messages are safe for clients; stack traces, SQL, paths, internal hostnames and secrets are excluded.

### Common status codes

| Status | Meaning |
| --- | --- |
| 200 | Successful read/update/command with a response body |
| 201 | Resource created; `Location` identifies it |
| 202 | Accepted for asynchronous processing; status resource is returned |
| 204 | Successful operation with no body |
| 400 | Malformed syntax, unsupported parameters or validation failure |
| 401 | Missing, expired or invalid authentication |
| 403 | Authenticated but not permitted |
| 404 | Resource absent or intentionally concealed |
| 409 | Uniqueness, state transition, idempotency or concurrency conflict |
| 412 | Failed `If-Match` precondition if ETag concurrency is selected |
| 413 | Upload/body exceeds allowed size |
| 415 | Unsupported or content-mismatched media type |
| 422 | Syntactically valid but semantically unacceptable command when distinguished from `400` |
| 429 | Rate limit exceeded; include safe retry guidance |
| 500 | Unexpected server error with correlation ID |
| 502/503/504 | Dependency failure/unavailability/timeout when the synchronous contract depends on it |

Clients must rely on status and stable `code`, not error prose.

## Authentication contracts

| Method and endpoint | Purpose | Access | Expected responses |
| --- | --- | --- | --- |
| `POST /auth/login` | Authenticate using the selected credential mechanism | Public | `200` session/token summary; `400`, `401`, `429` |
| `POST /auth/logout` | Invalidate the current session/token where supported | Authenticated | `204`; `401` |
| `POST /auth/refresh` | Rotate/renew an eligible session | Valid refresh/session context | `200`; `401`, `409`, `429` |
| `GET /auth/me` | Return current identity, roles and effective permissions | Authenticated | `200`; `401` |
| `POST /admin/users` | Create an inactive or policy-approved active user | `admin:users` | `201`; `400`, `401`, `403`, `409` |
| `POST /admin/users/{userId}/disable` | Disable a user and revoke all active sessions | `admin:users` | `200`; `400`, `401`, `403`, `404`, `409` last-admin protection |
| `PUT /admin/users/{userId}/roles/{role}` | Idempotently assign an allowed role | `admin:roles` | `204`; `400`, `401`, `403`, `404`, `409` |
| `DELETE /admin/users/{userId}/roles/{role}` | Remove a role under last-admin safeguards | `admin:roles` | `204`; `400`, `401`, `403`, `404`, `409` |

Conceptual login request:

```json
{ "username": "catalog.admin@example.test", "password": "<redacted>" }
```

Conceptual success body:

```json
{
  "user": { "id": "usr_...", "displayName": "Catalog Admin" },
  "roles": ["CATALOG_ADMIN"],
  "permissions": ["catalog:read", "catalog:write"],
  "expiresAt": "2026-08-16T16:00:00Z"
}
```

The final token/cookie response, CSRF protection and refresh design depend on the authentication ADR. Login failures use generic wording and cannot disclose account existence or status.

User disablement and any role/permission change classified as critical revoke affected active sessions after the change is durably accepted. User creation, disablement, role assignment/removal and last-administrator rejection require fail-closed audit acceptance. The exact session mechanism remains an ADR-006 decision.

## Product contracts

The access column below is the secured target contract. During the Phase 02 **LOCAL DEVELOPMENT PREVIEW**, real authentication/RBAC and the related `401`/`403` acceptance evidence remain pending; mutation endpoints must not be externally exposed. Phase 03 must close this gap before the External Exposure Gate can pass.

### Product representation

```json
{
  "id": "prd_...",
  "sku": "AEG-001",
  "name": "Reference Product",
  "description": "A catalog product.",
  "price": { "amount": "129.90", "currency": "BRL" },
  "stock": 12,
  "status": "ACTIVE",
  "categories": [{ "id": "cat_...", "name": "Reference" }],
  "links": { "images": "/api/v1/products/prd_.../images" },
  "version": 3,
  "createdAt": "2026-08-16T14:00:00Z",
  "updatedAt": "2026-08-16T15:00:00Z"
}
```

This is the Catalog-owned product representation and contains no Media-owned metadata. A client can follow the images link, or a future application/query composition response may combine Catalog and Media read models above both modules. Catalog never calls Media to build this resource.

| Method and endpoint | Purpose | Access | Expected responses |
| --- | --- | --- | --- |
| `POST /products` | Create a product | `catalog:write` | `201`; `400`, `401`, `403`, `409` duplicate SKU |
| `GET /products/{productId}` | Get an authorized product view | `catalog:read` | `200`; `401`, `403`, `404` |
| `GET /products` | Search/filter/page products | `catalog:read` | `200`; `400`, `401`, `403` |
| `PUT /products/{productId}` | Replace allowed product fields with concurrency check | `catalog:write` | `200`; `400`, `401`, `403`, `404`, `OPEN DECISION: 409 or 412` |
| `PATCH /products/{productId}` | Partially update explicitly supported fields | `catalog:write` | `200`; `400`, `401`, `403`, `404`, `OPEN DECISION: 409 or 412` |
| `POST /products/{productId}/deactivation` | Deactivate with explicit reason | `catalog:write` | `200`; `400`, `401`, `403`, `404`, `409` invalid state |
| `GET /products/{productId}/history` | Page product change history | `catalog:history:read` | `200`; `400`, `401`, `403`, `404` |
| `GET /products/{productId}/integration-status` | Inspect downstream synchronization | `catalog:read` | `200`; `401`, `403`, `404` |

`GET /products` supports initially:

- `query`: bounded name/SKU search text;
- `categoryId`;
- `status` (`ACTIVE`, `INACTIVE` where authorized);
- `page`, `size`, `sort` using an allowlist.

Creation accepts an optional `Idempotency-Key` if the implementation supports safely cached command outcomes. Duplicate SKU is always a conflict regardless of idempotency key.

## Category contracts

| Method and endpoint | Purpose | Access | Expected responses |
| --- | --- | --- | --- |
| `POST /categories` | Create category | `catalog:write` | `201`; `400`, `401`, `403`, `409` duplicate normalized name/slug |
| `GET /categories/{categoryId}` | Get category | `catalog:read` | `200`; `401`, `403`, `404` |
| `GET /categories` | Search/page categories | `catalog:read` | `200`; `400`, `401`, `403` |
| `PUT /categories/{categoryId}` | Update category with concurrency check | `catalog:write` | `200`; `400`, `401`, `403`, `404`, `OPEN DECISION: 409 or 412` |
| `POST /categories/{categoryId}/deactivation` | Deactivate under catalog policy | `catalog:write` | `200`; `400`, `401`, `403`, `404`, `409` when prohibited by use |

Category hierarchy endpoints are intentionally absent until a hierarchy requirement exists.

## Media contracts

| Method and endpoint | Purpose | Access | Expected responses |
| --- | --- | --- | --- |
| `POST /products/{productId}/images` | Validate and accept an image for asynchronous processing | `media:write` | `202`; `400`, `401`, `403`, `404`, `413`, `415`, `422` |
| `GET /products/{productId}/images` | List product image metadata | `media:read` | `200`; `401`, `403`, `404` |
| `GET /products/{productId}/images/{imageId}` | Get metadata and an authorized delivery reference | `media:read` | `200`; `401`, `403`, `404`, `409` not ready |
| `DELETE /products/{productId}/images/{imageId}` | Remove association and schedule safe object cleanup | `media:write` | `OPEN DECISION: 202 or 204`; `401`, `403`, `404`, `409` |
| `POST /products/{productId}/images/{imageId}/reprocessing` | Retry eligible failed processing | `media:write` | `202`; `400`, `401`, `403`, `404`, `409` invalid state |
| `PUT /products/{productId}/images/order` | Reorder images atomically | `media:write` | `200`; `400`, `401`, `403`, `404`, `OPEN DECISION: 409 or 412` |

Upload response:

```json
{
  "id": "img_...",
  "productId": "prd_...",
  "status": "PENDING",
  "statusUrl": "/api/v1/products/prd_.../images/img_...",
  "correlationId": "01J..."
}
```

The server generates the storage object name. Checks include authenticated permission, length, decoded content signature/type, safe dimensions, decompression limits and processing policy. Public object-store credentials/paths are never returned.

## Quality ingestion and query contracts

Ingestion endpoints are intended for approved CI/tools. They require a dedicated scoped identity, rate/size controls, schema version and an idempotency identity based on the **effective** source plus source execution identity.

The client-provided `source` field is a claim, not authority. The server derives the effective source from the authenticated service identity or validates the claim against that identity's allowlist. Accepted evidence binds, when available, the effective source to repository, workflow, commit SHA, immutable build identity and schema version. A payload cannot gain trust by naming a privileged source.

| Method and endpoint | Purpose | Access | Expected responses |
| --- | --- | --- | --- |
| `POST /quality/test-runs` | Ingest a run and result batch | `quality:ingest` | `202` accepted/duplicate-safe; `400`, `401`, `403`, `409`, `413`, `422` |
| `GET /quality/test-runs/{runId}` | Get normalized run and ingestion status | `quality:read` | `200`; `401`, `403`, `404` |
| `GET /quality/test-runs` | Filter runs by release, layer, status or source | `quality:read` | `200`; `400`, `401`, `403` |
| `POST /quality/security-findings` | Ingest normalized/raw supported security report | `quality:ingest` | `202`; `400`, `401`, `403`, `409`, `413`, `422` |
| `POST /quality/performance-results` | Ingest scenario metrics and report reference | `quality:ingest` | `202`; `400`, `401`, `403`, `409`, `413`, `422` |
| `POST /quality/defects` | Register/synchronize a defect reference | `quality:write` | `OPEN DECISION: 200 or 201`; `400`, `401`, `403`, `409` |
| `GET /quality/traceability/requirements/{requirementId}` | Return linked cases, executions, evidence, defects and releases | `quality:read` | `200`; `401`, `403`, `404` |

Conceptual test run envelope:

```json
{
  "schemaVersion": "1.0",
  "source": "github-actions/playwright",
  "sourceRunId": "123456789",
  "releaseVersion": "v0.6.0",
  "candidateId": "cand_...",
  "repository": "SYTEF/AEGIS",
  "workflow": "quality-validation",
  "build": { "commitSha": "<full-sha>", "artifactId": "backend-..." },
  "environment": "ci",
  "startedAt": "2026-08-16T14:00:00Z",
  "finishedAt": "2026-08-16T14:04:00Z",
  "results": [
    {
      "testCaseId": "TC-API-CAT-001",
      "status": "PASSED",
      "durationMs": 245,
      "attempt": 1,
      "evidence": []
    }
  ]
}
```

The ingest response returns the server-resolved `effectiveSource` and identifies accepted, rejected and duplicate records without falsely converting partial ingestion into full success. Maximum batch size and atomicity semantics must be fixed in the implementation contract.

Replay protection uses the authenticated effective source, source run ID, schema version and a canonical payload fingerprint within a versioned retention window. An exact replay returns the recorded idempotent outcome; reuse of the same identity with conflicting build/schema/payload returns `409`. Old or out-of-window submissions require an explicitly authorized import/reconciliation path and are audited. Artifact attestation/signature may strengthen provenance later but is not required by the foundation.

## Release contracts

| Method and endpoint | Purpose | Access | Expected responses |
| --- | --- | --- | --- |
| `POST /releases` | Create a logical release/version and scope | `quality:write` | `201`; `400`, `401`, `403`, `409` duplicate version |
| `GET /releases/{releaseId}` | Get release metadata | `quality:read` | `200`; `401`, `403`, `404` |
| `GET /releases` | Page/filter releases | `quality:read` | `200`; `400`, `401`, `403` |
| `PATCH /releases/{releaseId}` | Update allowed pre-finalization fields with concurrency check | `quality:write` | `200`; `400`, `401`, `403`, `404`, `OPEN DECISION: 409 or 412` |
| `POST /releases/{releaseId}/candidates` | Register an immutable build as a release candidate | `quality:write` | `201`; `400`, `401`, `403`, `404`, `409` duplicate/conflicting build identity |
| `GET /releases/{releaseId}/candidates/{candidateId}` | Get candidate/build and evidence-cutoff metadata | `quality:read` | `200`; `401`, `403`, `404` |
| `GET /releases/{releaseId}/candidates/{candidateId}/quality-summary` | Get evidence freshness and applicable gates; score/risk/recommendation may be `NOT_APPLICABLE` | `quality:read` | `200`; `401`, `403`, `404`, `409` evaluation unavailable |
| `POST /releases/{releaseId}/candidates/{candidateId}/evaluations` | Evaluate a fixed evidence cutoff using active versioned policy | `quality:write` | `OPEN DECISION: 201 or 202`; `400`, `401`, `403`, `404`, `409`, `422` |
| `GET /releases/{releaseId}/candidates/{candidateId}/traceability` | Get coverage and missing-link summary | `quality:read` | `200`; `401`, `403`, `404` |
| `POST /releases/{releaseId}/candidates/{candidateId}/decisions` | Record authorized final decision and rationale for the exact build | `release:decide` | `201`; `400`, `401`, `403`, `404`, `409`, `422` |

Conceptual quality summary:

```json
{
  "release": { "id": "rel_...", "version": "v1.0.0" },
  "candidate": { "id": "cand_...", "label": "rc.2", "buildId": "..." },
  "evidenceCutoff": "2026-08-16T15:00:00Z",
  "freshness": "CURRENT",
  "dimensions": {
    "functional": "PASS",
    "api": "PASS",
    "integration": "PASS",
    "e2e": "PASS",
    "security": "PASS",
    "performance": "PASS"
  },
  "qualityScore": { "value": 94, "maximum": 100, "formulaVersion": "1.0" },
  "risk": { "level": "LOW", "policyVersion": "1.0", "reasons": [] },
  "hardGates": [],
  "recommendation": "APPROVE",
  "finalDecision": null,
  "missingEvidence": []
}
```

A missing `finalDecision` is never equivalent to approval. A failed hard gate forces `BLOCK` recommendation even if `qualityScore.value` is high.

Before Quality Engine maturity, the summary returns `qualityScore`, `risk` and `recommendation` as `NOT_APPLICABLE` with the applicability policy reference; it does not return synthetic zeroes or infer approval. Definitions of release, candidate and build are in the [glossary](GLOSSARY.md).

## Idempotency and retries

- GET, PUT with the same full representation, and DELETE semantics should be idempotent where the domain permits.
- Eligible POST commands accept an `Idempotency-Key`; key scope, retention and body fingerprinting are documented per endpoint.
- Reusing a key with a different body returns `409 IDEMPOTENCY_CONFLICT`.
- A timed-out client may retry only according to endpoint policy; server processing must prevent duplicate effects.
- Message consumption is at-least-once and uses event identity for idempotency.

## Rate, size and abuse controls

Exact limits require baselining, but every endpoint must have bounded request/body sizes, pagination, timeout and concurrency/resource controls. Login, upload, expensive search, ingestion and Fault Lab controls receive dedicated protection. `429` responses may include `Retry-After` without revealing internal capacity.

## API security requirements

- Validate at transport, syntactic, semantic and domain layers.
- Bind resource authorization to the requested object, not only the endpoint.
- Use allowlists for sort fields, content types and state transitions.
- Prevent mass assignment by explicit command models.
- Do not expose JPA/domain internals directly as API models.
- Log safe security outcomes with correlation; never log credentials, raw bearer tokens, full upload contents or arbitrary evidence bodies.
- Protect browser cookie-based state changes against CSRF if cookies are selected.
- Publish security headers and CORS policy from an explicit allowlist.

## Contract evolution and testing

- OpenAPI becomes executable and reviewed with the first API implementation.
- Consumer/provider contract tests protect the Sales Center Mock boundary and event schemas.
- Backward compatibility checks run before merge when contracts change.
- Examples are test fixtures only after validation; documentation examples must not contain real secrets or personal data.
- Deprecation records replacement, impact, migration path and removal milestone.

## Open response-semantics decisions

Alternative status codes in the endpoint tables are explicitly unresolved and must be settled before executable OpenAPI is accepted:

- optimistic concurrency: `409 Conflict` versus `412 Precondition Failed`, coordinated with ADR-007;
- asynchronous deletion/cleanup: `202 Accepted` versus completed `204 No Content`;
- create-or-synchronize behavior: existing `200 OK` versus new `201 Created`;
- synchronous evaluation creation `201 Created` versus asynchronous `202 Accepted`;
- consistent boundary between `400 Bad Request` and `422 Unprocessable Content`;
- whether both PUT and PATCH are needed for Product or one update contract is sufficient.

No implementation may select whichever status makes a test pass. The decision must update examples, tests and OpenAPI consistently.

## Open contract questions

- Cookie session versus access/refresh tokens for the browser architecture.
- `ETag`/`If-Match` versus explicit body version for optimistic concurrency.
- One-category versus multi-category initial product command.
- Direct multipart upload versus controlled signed object upload.
- Evidence binary upload versus external artifact references.
- Ingestion batch atomicity and maximum report sizes.
- Exception semantics for a release with a critical hard block.
