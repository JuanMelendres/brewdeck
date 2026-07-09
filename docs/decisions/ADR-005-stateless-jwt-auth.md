# ADR-005: Stateless JWT authentication

## Status
Accepted

## Date
2026-07-09

## Context
Phase 6 introduces authentication. The API is consumed by a separate Next.js client and (for testing) Postman/Swagger, so a session-cookie server model adds friction. Public share links and auth entry points must remain open.

## Decision
Use **stateless JWT** (jjwt). A filter chain gates all `/api/**` except `/api/public/**`, `/api/auth/register`, and `/api/auth/login`. Passwords are hashed with **BCrypt**. Unauthenticated requests to protected routes return `401` JSON via a custom entry point. Secrets come from environment variables.

## Consequences
- **Positive:** no server-side session store; scales horizontally; simple for SPA clients.
- **Positive:** clear public/private route boundary; share links stay open.
- **Negative:** token revocation is non-trivial (no server session) — refresh tokens deferred to Slice C.
- **Negative:** token lifetime/rotation must be managed carefully.

## Alternatives Considered
- **Session cookies** — rejected: cross-origin friction with the SPA and API testing tools.
- **OAuth2 / external IdP** — deferred: overkill for the current single-app scope.

## Notes
Per-user ownership (owner FKs, per-user filtering) is a separate decision landing in Slice B. Sonar S4502 CSRF finding on the stateless chain is a known suppressed false positive.
