# BrewDeck — Claude Project Context

## Role

Act as a senior full-stack software engineer with strong experience in:
- Java 21
- Spring Boot 3
- PostgreSQL
- Flyway
- Spring Data JPA
- Testcontainers
- REST API design
- GitHub Actions
- SonarCloud
- OWASP Dependency Check
- Next.js / React / TypeScript

Prioritize maintainability, correctness, small incremental changes, and production-ready conventions.

## Project Summary

BrewDeck is a coffee brewing application.

The backend is a Spring Boot REST API used to manage:

- Coffees
- Brew methods
- Recipes
- Brew sessions

The project is currently backend-first. The frontend will be built later with Next.js.

## Current Backend Stack

- Java 21
- Spring Boot 3
- Maven Wrapper
- PostgreSQL 16
- Docker Compose
- Flyway
- Spring Data JPA
- Hibernate
- Bean Validation
- Testcontainers
- JUnit 5
- Mockito
- MockMvc
- JaCoCo
- Spotless
- OWASP Dependency Check
- SonarCloud
- GitHub Actions
- Springdoc OpenAPI / Swagger

## Current Architecture

Main backend packages:

- `coffee`
- `method`
- `recipe`
- `session`
- `common`
- `integration`

Current API resources:

- `/api/coffees`
- `/api/brew-methods`
- `/api/recipes`
- `/api/brew-sessions`

## Current Completed Features

The backend already includes:

- Full CRUD for Coffee
- Full CRUD for BrewMethod
- Full CRUD for Recipe
- Full CRUD for BrewSession
- PostgreSQL with Docker Compose
- Flyway migrations
- Initial brew methods seed
- GlobalExceptionHandler
- ErrorResponse
- RESTful status codes:
    - POST returns 201 Created
    - DELETE returns 204 No Content
    - GET, PUT, PATCH return 200 OK
- Recipe favorites:
    - `PATCH /api/recipes/{id}/favorite`
    - `PATCH /api/recipes/{id}/unfavorite`
    - `GET /api/recipes/favorites`
- Basic filters with Specification:
    - CoffeeFilter
    - RecipeFilter
    - BrewSessionFilter
- Standard paginated responses with `PageResponse`
- Request validation on:
    - CoffeeRequest
    - BrewMethodRequest
    - RecipeRequest
    - BrewSessionRequest
- Unit tests for services
- Controller tests
- Repository tests
- Specification repository tests
- Integration tests with Testcontainers
- JaCoCo coverage
- GitHub Actions
- SonarCloud

## Response Shape Rules

All GET endpoints that return collections must return `PageResponse<T>`:

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
GET by ID endpoints return the DTO directly.

## Pagination Rules
All collection GET endpoints should support:

- page
- size
- sort

Prefer safe default sorting while the project is evolving:
```java
@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC)
```

In integration tests, use explicit pagination params:
```java
.param("page", "0")
.param("size", "10")
.param("sort", "id,asc")
```

## Validation Rules

Use Bean Validation on request records.

Avoid special symbols in validation messages, especially °C, because error responses are sanitized and symbols may be escaped to HTML entities like &deg;.

Use:
```txt
degrees Celsius
```

instead of:

```txt
°C
```

## Error Handling

The project uses `GlobalExceptionHandler` and `ErrorResponse`.

Validation errors should return:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/example",
  "validationErrors": {
    "field": "message"
  }
}
```

## Testing Standards

Every meaningful backend change should include or update tests.

Use:

- Service tests for business logic
- Controller tests for HTTP behavior, validation, and status codes
- Repository tests for custom queries
- Specification repository tests for filters
- Integration tests for complete workflows

Avoid fragile assertions that assume an integration test database has only one record unless the test fully controls the dataset.

For paginated responses, assert:

```java
jsonPath("$.content")
jsonPath("$.content[0].id")
jsonPath("$.page")
jsonPath("$.size")
jsonPath("$.totalElements")
```
Do not assert $[0] for paginated endpoints.

## Quality Commands

Run these before considering a task done:

For Windows PowerShell or CMD:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean verify
```

For macOS/Linux:

```bash
./mvnw spotless:apply
./mvnw clean verify
```

Focused test on Windows:

```bash
.\mvnw.cmd -Dtest=ClassName test
```

Focused test on macOS/Linux:

```bash
./mvnw -Dtest=ClassName test
```

## Git Rules

Use Conventional Commits.

Examples:

- `feat(api): add request validation for brew sessions`
- `fix(api): correct paginated favorites integration test`
- `test(api): update controller tests for validation errors`
- `refactor(api): reduce duplication in recipe service`
- `docs(api): update backend roadmap`
- `ci(api): improve sonar workflow`
- `style(api): format code without functional changes`
- `chore(api): update build scripts or dependencies`

When suggesting a commit, always provide one Conventional Commit message.

## Coding Style

- Keep changes small and focused.
- Do not refactor unrelated areas.
- Do not remove tests unless they are obsolete and replaced by better coverage.
- Prefer readable code to clever abstractions.
- Keep DTOs explicit.
- Use records for request/response/filter objects when appropriate.
- Keep controllers thin.
- Keep business logic in services.
- Keep persistence logic in repositories/specifications.
- Avoid leaking entity objects directly from controllers.

## Security and Quality

Never hardcode real secrets.

Use environment variables for credentials.

Use `.env.example`, not `.env`, in Git.

Keep OWASP Dependency Check and SonarCloud passing unless explicitly working on known false positives.

## Postman

The project includes a Postman collection for manual API testing.

Location:

```text
docs/postman/brewdeck.postman_collection.json
docs/postman/brewdeck.local.postman_environment.json
```

## Postman Rules
- Keep Postman requests aligned with the current REST API.
- Use Long ID variables, not `{{$guid}}`.
- Use:
  - `{{coffeeId}}`
  - `{{methodId}}`
  - `{{recipeId}}`
  - `{{sessionId}}`
- Do not commit real credentials or tokens.
- Prefer environment variables for base URLs.
- Update the collection when endpoints change.

### Current Phase

Current phase:
Backend stabilization and frontend preparation.

Recently completed:

- Pagination standardized across GET collection endpoints.
- Controller, service, repository, specification, and integration tests updated.
- Request validations improved for Coffee, BrewMethod, Recipe, and BrewSession.
- Postman collection updated for current API endpoints.

Next recommended tasks:

1. Run full verification.
2. Review JaCoCo and SonarCloud.
3. Add CORS for Next.js.
4. Add basic structured logs in services.
5. Add dashboard summary endpoint.
6. Improve Swagger/OpenAPI docs.
7. Start Next.js frontend.

## Important Instruction

Before making changes:

1. Inspect current files.
2. Identify the smallest safe change.
3. Explain what will change.
4. Apply code changes.
5. Update or add tests.
6. Run or suggest the exact verification command.
7. Provide a Conventional Commit message.