# Unit Testing

## Backend

- **Framework:** JUnit 5 + Mockito; MockMvc for controller slices.
- **Scope:** one class under test, collaborators mocked.
- **Cover:** service business rules, mappers, validators, error/exception paths, controller status codes and validation.
- **Structure:** Arrange–Act–Assert; one behavior per test; descriptive method names.

Example targets:
- Service: happy path, not-found (`404`), conflict (`409`), unprocessable (`422`, e.g. AI improve without rated history).
- Controller: request validation (`400`), success status codes (201/200/204), paginated JSON shape.

## Frontend

- **Framework:** Vitest + React Testing Library.
- **Cover:** component behavior, form validation (Zod), loading/error/empty states, hook logic.
- **Mock:** the API-client layer or the corresponding hook — not child components (generally).
- **Queries:** prefer `getByRole` / `getByText` / `getByLabelText`.

## Conventions

- Colocate `*.test.tsx` next to the unit it covers (frontend).
- Never assert `$[0]` on paginated bodies — use `$.content[0]`.
- Keep tests deterministic; no reliance on external services (AI feature stays off).
