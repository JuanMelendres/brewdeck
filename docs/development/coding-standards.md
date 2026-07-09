# Coding Standards

The authoritative, detailed rules live in [`../../CLAUDE.md`](../../CLAUDE.md) and
[`.claude/conventions.md`](../../.claude/conventions.md). This is the summary.

## Backend (Java / Spring Boot)

- Package-by-domain (see [ADR-001](../decisions/ADR-001-modular-monolith-package-by-domain.md)).
- Never expose entities from controllers — map to explicit DTOs/records.
- Bean Validation on all `*Request` records; no special symbols in messages (sanitized).
- Collection GETs return `PageResponse<T>`; GET-by-id returns the DTO; bounded analytics return `List<T>`.
- RESTful status codes: POST 201, DELETE 204, GET/PUT/PATCH 200.
- Format with Spotless before committing; keep PMD and SonarCloud green.

## Frontend (Next.js / React / TypeScript)

- Strict TypeScript — no `any`; precise interfaces or `unknown` + guards.
- Functional components + hooks only. Named exports everywhere except `page.tsx`/`layout.tsx` (which need `export default`).
- Server state via TanStack Query; never store server data in global client state. Centralize query keys; invalidate the right prefixes.
- Forms via React Hook Form + Zod; mirror backend limits; map server `validationErrors` back onto fields.
- Absolute imports via the `@/` alias; group React → third-party → internal.
- Always handle loading, error, and empty states.

## Cross-cutting

- Never hardcode secrets — use env vars; commit `.env.example` only.
- Small, incremental, well-tested changes.

## Anti-patterns to avoid

- Leaking entities · using `any` · server data in global client state · asserting `$[0]` on paginated bodies · special symbols in validation messages · running `lint:fix` on the whole repo (scope to changed files).
