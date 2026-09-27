# ADR-011: In-memory rate limiting for auth endpoints

## Status
Accepted

## Date
2026-09-26

## Context
The 2026-09-24 backend audit found no throttling on the public auth endpoints: 12 consecutive failed logins all returned `401` with no slowdown, which leaves online password guessing unrestricted. Forgot-password could also be used to flood someone's inbox with reset emails. BrewDeck runs as a **single instance** with one main user for now (owner decision, 2026-09-26).

## Decision
- **Store:** an in-process, fixed-window counter (`RateLimiter`) on top of Caffeine, which is already a dependency (ADR-007 caching). No new library or infrastructure.
- **Per client IP:** `AuthRateLimitFilter`, in the security chain before JWT handling, covers `POST` on login, register, refresh, forgot-password, reset-password, and verify-email.
- **Per account:** services apply limits by email. Login has one, so rotating IPs does not help against one account. Forgot-password has one, checked before the user lookup so known and unknown emails are throttled alike (no enumeration).
- **Limits** (`RateLimitRule`):

  | Rule | Limit |
  |---|---|
  | `LOGIN_IP` | 20 / minute |
  | `LOGIN_EMAIL` | 10 / 15 minutes |
  | `REGISTER_IP` | 5 / hour |
  | `REFRESH_IP` | 30 / minute |
  | `FORGOT_PASSWORD_IP` | 5 / 15 minutes |
  | `FORGOT_PASSWORD_EMAIL` | 3 / hour |
  | `RESET_PASSWORD_IP` | 10 / 15 minutes |
  | `VERIFY_EMAIL_IP` | 10 / 15 minutes |

- **Response:** `429 Too Many Requests` with `Retry-After` (seconds) and the standard `ErrorResponse` body (`"Too many attempts. Try again in N minutes."`). The frontend shows that message on login, register, and forgot-password.
- **Client IP:** `HttpServletRequest.getRemoteAddr()`. Raw `X-Forwarded-For` is never read, because clients can spoof it. Behind a reverse proxy, set `server.forward-headers-strategy` so the container resolves the real client address.
- **Switch:** `brewdeck.rate-limit.enabled` (`RATE_LIMIT_ENABLED`, default `true`). It is `false` only in the `test` profile, because every integration test shares one client IP. `RateLimitIntegrationTest` turns it back on. This is configuration, not a feature flag, and must stay on in every real environment.

## Consequences
- **Positive:** caps online guessing at 10 attempts per account per 15 minutes; bounds reset-mail volume; adds no dependency or infrastructure.
- **Negative:** state is per JVM and lost on restart. **With more than one instance, each instance would allow the full limit.** That is acceptable for the current single-instance deployment and must be revisited before scaling out.
- **Negative:** the per-account login limit lets someone who knows your email lock you out of password login for up to 15 minutes. This is the usual trade-off of account-level limits; acceptable at current scale.
- **Negative:** a fixed window allows up to twice the limit across a window boundary. A token bucket would smooth this, but it is not worth the extra complexity here.

## Alternatives Considered
- **Bucket4j:** a proper token bucket with a Redis backend available later. Deferred: it adds a dependency for no gain at one instance, and moving to it later only means reimplementing `RateLimiter`.
- **Redis-backed limiter:** needed for multiple instances. Rejected for now: new infrastructure for a single-instance app.
- **Reverse-proxy limiting (nginx, API gateway):** it has no deployment yet, and it cannot key by account email.
- **Account lockout after N failures:** a harsher version of the per-account limit, and it needs an unlock flow.

## Notes
The AI endpoints (`/api/recipes/suggest`, `/{id}/improve`) cost money per call and should get a per-user limit when the AI assistant is re-enabled (see the AI suspension note in `.claude/project-state.md`).
