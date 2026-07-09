# Integration Testing

Integration tests exercise the API end to end against a real PostgreSQL via Testcontainers, including Flyway migrations, Specifications, and constraints.

## Setup

- **Engine:** PostgreSQL 16 in a Testcontainers-managed container (Docker required).
- **Location:** `brewdeck-api/src/test/.../integration/`.
- **Migrations:** Flyway runs against the container, so migrations are validated on every run.

## Rules

- Use **explicit pagination params**: `.param("page", "0").param("size", "10").param("sort", "id,asc")`.
- Do **not** assume a single record exists in a shared dataset — assert `greaterThanOrEqualTo`, or fully control the data you insert.
- Assert on the envelope: `$.content`, `$.content[0].id`, `$.page`, `$.size`, `$.totalElements`.
- Cover workflows, not just single calls (e.g. create → fetch → update → delete).

## Example coverage

- CRUD round-trips per domain.
- Filter/Specification queries.
- Favorites and share/unshare transitions.
- Auth gate: `401` without a token on protected routes; open access to `/api/public/**` and auth endpoints.
- OpenAPI docs endpoint responds (see `OpenApiDocsIntegrationTest`).

## Running

```bash
cd brewdeck-api
./mvnw clean verify         # includes integration tests
```

See [ADR-003](../decisions/ADR-003-testcontainers-integration-testing.md) for the rationale.
