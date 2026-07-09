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
  "validationErrors": { "field": "message" }
}
```

| Status | Meaning |
| ------ | ------- |
| 400 | Validation / malformed request |
| 401 | Missing or invalid JWT |
| 404 | Resource not found |
| 409 | Conflict (e.g. duplicate) |
| 422 | Unprocessable (e.g. AI improve with no rated history) |
| 503 | AI feature disabled or provider unavailable |
| 500 | Unexpected error |

## Auth

Stateless JWT. All `/api/**` require a `Bearer` token except `/api/public/**`,
`/api/auth/register`, and `/api/auth/login`. See [ADR-005](../decisions/ADR-005-stateless-jwt-auth.md).

## Docs & tooling

- Live Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI seed: [`docs/api/openapi.yaml`](../api/openapi.yaml)
- Postman: [`docs/api/postman/`](../api/postman/)
