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
- **Ownership, reads and writes:** another user's coffee, recipe, or session is a `404` on every read (`CrossUserIsolationIntegrationTest`) and every write, reference, favorite, or share (`CrossUserWriteIsolationIntegrationTest`), and the foreign row stays unchanged. Brew-method tiers: `BrewMethodOwnershipIntegrationTest`.
- **Public sharing:** share → anonymous curated read → unshare/re-share kills the old link (`RecipeShareFlowIntegrationTest`).
- **JWT edge cases:** expired, tampered, foreign-key, `alg: none`, deleted-user, malformed, and wrong-scheme tokens are all `401` (`JwtEdgeCasesIntegrationTest`).
- **Error mapping and limits:** `MvcErrorHandlingIntegrationTest`, `ValidationLimitsIntegrationTest`, `RateLimitIntegrationTest` (the only one with the rate limiter enabled).
- OpenAPI docs endpoint responds (see `OpenApiDocsIntegrationTest`).

When a test needs another user's data, seed it through repositories with a second `User`, then call the API as the mock user and assert both the status and that the foreign row is unchanged.

## Running

```bash
cd brewdeck-api
./mvnw clean verify         # includes integration tests
```

See [ADR-003](../decisions/ADR-003-testcontainers-integration-testing.md) for the rationale.
