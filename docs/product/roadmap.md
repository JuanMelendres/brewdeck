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
| 6 | Auth & multi-user | Completed |
| 7 | UI/UX refresh (design system, navigation, dashboard, lists, auth screens, brand mark, i18n) | In progress — visual workstreams done; i18n (workstream 8) pending |

## Phase 6 breakdown

- **Slice A — Auth foundation:** self-registration, JWT login, gate all `/api/**` (public share + auth endpoints open). — **Done**
- **Slice B — Per-user ownership:** owner FK on coffees/recipes/sessions, per-user filtering + data migration. — Done (B.1 owner_id FK + create-time stamping; B.2 all reads owner-scoped + owner_id NOT NULL via V7)
- **Slice C — Account UX:** email verification, password reset, refresh tokens, profile. — Done
  - C.1 — Profile management (display name) + password change (`PATCH /api/auth/me`, `POST /api/auth/change-password`). — Done
  - C.2 — Password reset via hashed single-use tokens + mail port (`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`). — Done
  - C.3 — Email verification (soft gate): `email_verified` flag, hashed 24h tokens, register-time issue, banner + `/verify-email` (`POST /api/auth/verify-email`, `POST /api/auth/resend-verification`). — Done
  - C.4 — Refresh tokens (store + rotation): hashed single-use tokens (Flyway V11), reuse-detection revokes all active tokens, `POST /api/auth/refresh` + `POST /api/auth/logout`, 15m access-token TTL, frontend silent refresh + server-revoking logout. — Done (PR #76)

**Released:** Phase 6 shipped to `master` via PR #77 (develop→master), bundling Slices C.1–C.4 plus the pnpm migration and CI overhaul. All CI green; develop remains the default branch.

## Phase 7 breakdown

Spike: [ui-ux-refresh-spike](spikes/ui-ux-refresh-spike.md). Decision record: [ADR-014](../decisions/ADR-014-ui-design-system-mui-theme-tokens.md).

| # | Workstream | Status |
| --- | --- | --- |
| 1 | Design foundation: theme tokens, fonts, dark mode ([TDD](../architecture/design-foundation-tdd.md)) | Done (#173, #187, #188) |
| 2 | Navigation: icons, active route, mobile drawer ([FDD](fdd/navigation-fdd.md)) | Done (#177) |
| 3 | Dashboard: greeting, stat cards, rankings | Done (#181) |
| 4 | Lists: card grids, airier tables, skeletons, empty states | Done (#182, #183, #184) |
| 5 | Auth screens: split layout with brand panel | Done (#180) |
| 6 | Micro-interactions: toasts, card hover | Done (#186) |
| 7 | Theme preference: per-user light/dark, first-login dialog ([FDD](fdd/theme-preference-fdd.md)) | Done (#175, #176) |
| 8 | Language (i18n): Spanish + English | Spike in progress |
| 9 | Forms: sectioned dialogs, select-label fix ([FDD](fdd/forms-fdd.md)) | Done (#178, #179) |
| 10 | Brand mark: logo, wordmark, app icons, home-screen install ([spike](spikes/brand-mark-spike.md)) | Done (#189, #190) |

**Released:** workstreams 2–5, 7, and 9 shipped to `master` in #185; the rest go out in the next release.

## Status

Phases 1–6 are Completed; Phase 7 is in progress (only i18n remains). Ongoing work past this point is maintenance (CVE/dependency remediation) and follow-ups tracked in [`.claude/project-state.md`](../../.claude/project-state.md) "Immediate Next Steps" (e.g. moving auth tokens off `localStorage` to `httpOnly` cookies). The AI recipe assistant (Phase 5) remains built but feature-flagged off (paused, not removed). "Vision" below is unscheduled future scope, not counted toward this roadmap.

## Vision (post-roadmap)

- Hardware integration (e-paper device), offline sync, advanced analytics.

> Keep this table in sync with `.claude/roadmap.md` whenever a phase changes status.
