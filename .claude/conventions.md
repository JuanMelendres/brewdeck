# BrewDeck Engineering Conventions

## Java

- Use Java 21.
- Prefer records for DTOs and request/filter objects.
- Keep controllers thin.
- Keep services focused on business logic.
- Keep repositories focused on persistence.
- Use Specifications for flexible filters.
- Use Bean Validation for request validation.
- Use `PageResponse<T>` for paginated API responses.

## Spring Boot

- Use constructor injection.
- Prefer `@RequiredArgsConstructor`.
- Use `@RestControllerAdvice` for global errors.
- Use `ResponseEntity` for status control.
- Use `@Valid` on request bodies.
- Use `@PageableDefault` on collection GET endpoints.

## Testing

- Use JUnit 5.
- Use Mockito for service/controller unit tests.
- Use MockMvc for controller tests.
- Use Testcontainers for integration/repository tests.
- Test validation error body, not only status code.
- Do not rely on database test ordering unless explicitly sorting.
- Use unique test values when constraints are unique.

## API

- POST returns 201 Created.
- DELETE returns 204 No Content.
- GET/PUT/PATCH return 200 OK.
- Collection endpoints return PageResponse.
- Detail endpoints return DTO directly.
- Error responses are standardized.

## Git

Always use Conventional Commits.

Allowed prefixes:

- feat
- fix
- test
- refactor
- docs
- ci
- chore
- perf
- style

Examples:

- `feat(api): add dashboard summary endpoint`
- `test(api): add validation coverage for brew sessions`
- `fix(api): stabilize favorites integration test`
- `refactor(api): extract recipe lookup helpers`