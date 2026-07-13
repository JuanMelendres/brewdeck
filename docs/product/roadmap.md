# Roadmap

This is a stable, high-level summary. The living, detailed roadmap is
[`.claude/roadmap.md`](../../.claude/roadmap.md); current status lives in
[`.claude/project-state.md`](../../.claude/project-state.md).

| Phase | Theme | Status |
| ----- | ----- | ------ |
| 1 | Backend foundation (Spring Boot, PostgreSQL, Docker, Flyway, CRUD) | Completed |
| 2 | Backend quality (unit/controller/repo/integration tests, JaCoCo, Spotless, SonarCloud, Dependency Check, CI) | Completed |
| 3 | Backend UX for frontend (PageResponse, filters, favorites, CORS, dashboard, OpenAPI) | Completed |
| 4 | Frontend (Next.js + React + TypeScript + MUI + TanStack Query + Vitest) | Completed |
| 5 | Product improvements (analytics widgets, tasting radar, AI suggestions/improve, PDF export, public share links) | Completed |
| 6 | Auth & multi-user | In progress |

## Phase 6 breakdown

- **Slice A — Auth foundation:** self-registration, JWT login, gate all `/api/**` (public share + auth endpoints open). — **Done**
- **Slice B — Per-user ownership:** owner FK on coffees/recipes/sessions, per-user filtering + data migration. — Done (B.1 owner_id FK + create-time stamping; B.2 all reads owner-scoped + owner_id NOT NULL via V7)
- **Slice C — Account UX:** email verification, password reset, refresh tokens, profile. — In progress
  - C.1 — Profile management (display name) + password change (`PATCH /api/auth/me`, `POST /api/auth/change-password`). — Done
  - C.2 — Password reset via hashed single-use tokens + mail port (`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`). — Done
  - C.3 — Email verification (soft gate): `email_verified` flag, hashed 24h tokens, register-time issue, banner + `/verify-email` (`POST /api/auth/verify-email`, `POST /api/auth/resend-verification`). — Done
  - C.4 — Refresh tokens (store + rotation). — Pending

## Vision (post-roadmap)

- Hardware integration (e-paper device), offline sync, advanced analytics.

> Keep this table in sync with `.claude/roadmap.md` whenever a phase changes status.
