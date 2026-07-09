# Contribution Guide

Lightweight workflow for a small, quality-gated project.

## Workflow

1. Branch from `develop` (default working branch; `master` is the main/stable branch).
2. Make a small, focused change with tests.
3. Run the local gates (below).
4. Open a PR into `develop`.
5. Merge only when CI is green (see [test-plan.md](../testing/test-plan.md)).

## Branch naming

`<type>/<short-topic>`, e.g. `feature/auth-foundation`, `docs/organize-docs-as-code`, `fix/null-average-rating`.

## Commit convention

Conventional Commits, one message per commit. Scopes: `api` (backend), `web` (frontend), plus `docs`/`test`/`ci`/`chore`/`refactor`/`style`/`fix`/`perf`.

Examples:
- `feat(api): add most-used coffees analytics endpoint`
- `feat(web): add top-rated recipes widget to the dashboard`
- `fix(web): guard against a null average rating`
- `docs: organize documentation as docs-as-code`

## Before pushing

```bash
# backend
cd brewdeck-api && ./mvnw spotless:apply && ./mvnw clean verify && ./mvnw pmd:check

# frontend
cd brewdeck-web && npm run test && npm run type-check
```

## Documentation

- Update docs in the same PR as the code they describe.
- Keep [`docs/api/`](../api/README.md), [`openapi.yaml`](../api/openapi.yaml), and the Postman collection in sync with the controllers.
- Update [`docs/product/roadmap.md`](../product/roadmap.md) when a phase changes status; keep `.claude/roadmap.md` and `.claude/project-state.md` current.
- Add an ADR under [`docs/decisions/`](../decisions/) for decisions affecting architecture, tooling, persistence, deployment, or long-term maintainability.

## Git hooks

- Pre-commit → format + tests
- Pre-push → full validation + security scan
