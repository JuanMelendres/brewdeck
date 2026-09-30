# API Design

Design principles and conventions for the BrewDeck REST API. The full endpoint
catalog and machine-readable spec live in [`docs/api/`](../api/README.md).

## Principles

- RESTful resources under `/api/**`; nouns, plural collections.
- Explicit DTOs/records in and out — entities are never exposed.
- Consistent status codes: POST 201, DELETE 204, GET/PUT/PATCH 200.
- Validation via Bean Validation on request records; errors via `GlobalExceptionHandler`.

## Pagination envelope

Collection GETs return `PageResponse<T>` and accept `page`, `size`, `sort`:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

- GET-by-id returns the DTO directly (not wrapped).
- Bounded analytics rankings (top-rated, most-brewed, most-used, usage) return a plain `List<T>` — they are top-N, not browsable collections.

## Error shape

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/example",
  "validationErrors": { "field": "message" },
  "code": null
}
```

`code` is an optional machine-readable reason, set only where a client must react to one specific error (currently `EMAIL_NOT_VERIFIED`).

| Status | Meaning |
| ------ | ------- |
| 400 | Validation / malformed request / missing or mistyped parameter |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but not allowed (e.g. non-admin on `/api/admin/**`, editing a shared brew method). With `"code": "EMAIL_NOT_VERIFIED"`: verify your email first ([ADR-012](../decisions/ADR-012-require-email-verification.md), only while that flag is on) |
| 404 | Resource not found, or no such endpoint |
| 405 | HTTP method not supported on that path (`Allow` header lists the valid ones) |
| 406 | Requested `Accept` type cannot be produced |
| 409 | Conflict: a duplicate, or a delete blocked because other rows still use the resource (the message says what, e.g. `"Coffee is used by 2 recipes. Delete or change them first."`) |
| 415 | Unsupported `Content-Type` (send `application/json`) |
| 422 | Unprocessable (e.g. AI improve with no rated history) |
| 429 | Too many attempts on an auth endpoint; wait `Retry-After` seconds ([ADR-011](../decisions/ADR-011-in-memory-auth-rate-limiting.md)) |
| 503 | AI feature disabled or provider unavailable |
| 500 | Unexpected error: the body stays generic, and the full stack trace is logged server-side |

Spring MVC's own client errors (405/404/415/406/400 and `ResponseStatusException`) keep their real
status and headers in this same error shape. Only a truly unexpected exception becomes a `500`.

## Auth

Stateless JWT. All `/api/**` require a `Bearer` token except `/api/public/**`,
`/api/auth/register`, and `/api/auth/login`. See [ADR-005](../decisions/ADR-005-stateless-jwt-auth.md).

## Docs & tooling

- Live Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI seed: [`docs/api/openapi.yaml`](../api/openapi.yaml)
- Postman: [`docs/api/postman/`](../api/postman/)
