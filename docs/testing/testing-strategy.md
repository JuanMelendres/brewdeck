# Testing Strategy

BrewDeck favors fast, isolated tests plus realistic integration tests. Every meaningful change ships with tests, following Arrange–Act–Assert.

## Backend layers

| Layer | Tooling | Covers |
| ----- | ------- | ------ |
| Unit — service | JUnit 5 + Mockito | Business logic, mapping, error paths |
| Controller | MockMvc | HTTP status, validation, JSON shape |
| Repository | Spring Data test slice | Custom queries |
| Specification | Unit | Filter predicates |
| Integration | Testcontainers (PostgreSQL 16) | End-to-end workflows against real SQL |

Coverage is measured with **JaCoCo**; quality gates run in **SonarCloud**. DTO/adapter classes are excluded from coverage (mirrored in `sonar.coverage.exclusions`).

## Frontend

**Vitest + React Testing Library.** Test behavior, not implementation. Prefer accessible queries (`getByRole`, `getByText`, `getByLabelText`); avoid `getByTestId` unless necessary. Mock the API-client layer / hooks; wrap query hooks in a `QueryClientProvider`.

## Golden rules

- Paginated bodies: assert `$.content`, `$.content[0].id`, `$.page`, `$.size`, `$.totalElements` — never `$[0]`.
- Integration tests use explicit `page`/`size`/`sort` and never assume a single record in a shared DB (assert `>=`).
- No special symbols in validation messages (responses are sanitized).

## What's covered vs. missing

- **Covered:** CRUD services/controllers, repositories, specifications, integration workflows, OpenAPI docs test.
- **By design untested:** live AI SDK path (feature off by default; adapter Sonar-excluded).
- **`TODO`:** frontend coverage gaps and deferred test nits tracked in `.claude/project-state.md` / `.superpowers/sdd/progress.md`.

## Related

- [Test plan](test-plan.md) · [Unit testing](unit-testing.md) · [Integration testing](integration-testing.md)
- [ADR-003: Testcontainers](../decisions/ADR-003-testcontainers-integration-testing.md)
