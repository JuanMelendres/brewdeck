# BrewDeck Roadmap

## Phase 1 — Backend Foundation

Status: Completed

- Spring Boot API
- PostgreSQL
- Docker Compose
- Flyway
- CRUD resources
- DTOs
- Validations
- Error handling
- RESTful status codes

## Phase 2 — Backend Quality

Status: Mostly Completed

- Unit tests
- Controller tests
- Repository tests
- Integration tests
- Testcontainers
- JaCoCo
- Spotless
- SonarCloud
- Dependency Check
- GitHub Actions

## Phase 3 — Backend UX for Frontend

Status: Completed

- PageResponse for lists
- Query filters
- Favorites
- CORS
- Dashboard summary
- Better Swagger/OpenAPI docs
- Basic logs

## Phase 4 — Frontend

Status: Completed

Stack (in use):

- Next.js
- React
- TypeScript
- Tailwind / shadcn/ui
- API client layer
- Forms with validation (zod schemas)
- TanStack Query mutation hooks
- Vitest

Screens:

- Dashboard — Done
- Coffees list/create/edit/delete — Done (PR #32)
- Recipes list/create/edit/delete — Done
- Recipes detail (+ brew stats) — Done (PR #35)
- Brew sessions list — Done
- Brew sessions create — Done
- Brew session history by recipe — Done
- Brew methods list — Done
- Favorite recipes (dedicated screen) — Done

## Phase 5 — Product Improvements

Status: Completed

- Top-rated recipes (endpoint + dashboard widget) — Done (PRs #38, #39)
- Most-brewed recipes (endpoint + widget) — Done (PRs #40, #41)
- Brew method usage breakdown (endpoint + widget) — Done (PRs #43, #44)
- Most-used coffees (endpoint + widget) — Done (PRs #46, #47)
- Rating trend over time (chart, recipe detail) — Done (PR #50)
- Recommended grind adjustments (recipe detail hint) — Done (PR #54)
- Coffee tasting notes visualization (radar chart on coffee detail) — Done (PR #57)
- AI-assisted recipe suggestions — Done: generate slice (PR #58) + improve-from-history slice (POST /api/recipes/{id}/improve, "Improve with AI" on recipe detail)
- Export recipes to PDF — Done: client-side "Export PDF" on recipe detail (jspdf recipe card)
- Public share links (opt-in revocable token, public /share/[token] page) — Done

## Phase 6 — Auth & Multi-User

Status: Completed

- Auth foundation (Slice A) — self-registration, JWT login, gate all /api/** (public share + auth endpoints open) — Done
- Per-user ownership (Slice B) — Done
  - B.1 (write path) — owner_id FK (Flyway V6) on coffees/recipes/sessions, backfill, CurrentUserProvider, stamp owner on create — Done
  - B.2 (read path) — per-user filtering on all reads (CRUD, favorites, session-by-recipe, analytics, dashboard, AI improve; share/unshare owner-scoped, public token read stays global) + owner_id NOT NULL (Flyway V7) — Done
- Account UX (Slice C) — split into sub-slices — Done
  - C.1 (profile + password change) — display_name (Flyway V8), PATCH /api/auth/me, POST /api/auth/change-password, /account page (ProfileForm + ChangePasswordForm) — Done
  - C.2 (password reset) — hashed single-use tokens (Flyway V9), PasswordResetMailPort (logging default, SMTP stub behind brewdeck.mail.enabled), public POST /api/auth/forgot-password (always 200, no enumeration) + POST /api/auth/reset-password (204), /forgot-password + /reset-password pages — Done
  - C.3 (email verification) — email_verified flag (Flyway V10, existing backfilled verified), hashed single-use 24h tokens (email_verification_tokens), EmailVerificationMailPort reusing the C.2 mail toggle, register-time issue hook, public POST /api/auth/verify-email (204) + authenticated POST /api/auth/resend-verification (200), soft gate (login unaffected; /me exposes emailVerified), frontend banner + /verify-email page — Done
  - C.4 (refresh tokens) — hashed single-use refresh tokens (Flyway V11), rotation with reuse-detection (revokes all active tokens for the user), `POST /api/auth/refresh` (200) + `POST /api/auth/logout` (204), access-token TTL shortened to 15m, frontend silent single-flight refresh + server-revoking logout — Done (PR #76)

Released to master: Phase 6 shipped develop→master via PR #77 (Slices C.1 #73, C.2 #74, C.3 #75, C.4 #76 + pnpm migration + CI overhaul); all CI green, develop kept as the default working branch.