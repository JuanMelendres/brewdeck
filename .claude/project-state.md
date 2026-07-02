# BrewDeck Project State

## Last Updated

2026-07-02

## Current Phase

Backend hardening before frontend.

## Completed

- Java 21 Spring Boot backend
- PostgreSQL Docker setup
- Flyway migrations
- Initial brew method seed
- CRUD endpoints
- RESTful status codes
- Global exception handling
- Request validation
- Filters with Specification
- Standard PageResponse
- Pagination for collection GET endpoints
- Recipe favorites
- Service tests
- Controller tests
- Repository tests
- Specification tests
- Integration tests with Testcontainers
- JaCoCo
- Spotless
- SonarCloud
- GitHub Actions

## Recently Worked On

- Request validation hardening
- Controller tests for validation errors
- Integration test stabilization for paginated responses
- Favorite workflow integration test correction

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Run full verification (use .cmd suffix if explicitly on Windows CMD):
    - `./mvnw spotless:apply`
    - `./mvnw clean verify`
2. Review JaCoCo report.
3. Review SonarCloud.
4. Add CORS config for Next.js.
5. Add service logs.
6. Add dashboard summary endpoint.