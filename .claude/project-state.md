# BrewDeck Project State

## Last Updated

2026-07-03

## Current Phase

Phase 3 (Backend UX for Frontend) completed. Ready to start Phase 4 (Next.js frontend).

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
- CORS config for Next.js
- Dashboard summary endpoint
- Structured service logs for write operations
- OpenAPI/Swagger documentation

## Recently Worked On

- CORS configuration (WebConfig) for the Next.js frontend
- Dashboard summary endpoint (GET /api/dashboard/summary)
- Structured INFO logs on service write operations
- Enriched OpenAPI/Swagger docs (tags, per-endpoint operations)

## Known Rules

- Collection GET endpoints return PageResponse.
- GET by ID returns DTO.
- Integration tests must use explicit page/size/sort.
- Avoid assuming only one record exists in shared integration test DB.
- Avoid special characters in validation messages because responses are sanitized.

## Immediate Next Steps

1. Push pending backend commits (dashboard, Swagger, service logs).
2. Review JaCoCo and SonarCloud after push.
3. Start Phase 4: scaffold Next.js frontend.
4. Build API client layer against the documented endpoints.
5. Implement dashboard page consuming GET /api/dashboard/summary.