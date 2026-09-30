# ADR-013: Refresh token in an httpOnly cookie, access token in memory

## Status
Accepted (rollout in 3 PRs; see Plan)

## Date
2026-10-01

## Context
The web client kept both tokens in `localStorage`. Any XSS, including one introduced by a future dependency, could read the **refresh token** and keep a session alive for up to 7 days (`AUTH_REFRESH_TTL`) from another machine, even after the tab closes. The access token is short-lived (15 min), so the refresh token is the valuable secret.

## Decision
- **Refresh token → httpOnly cookie** `brewdeck_refresh`: `HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=<refresh TTL>`. Page scripts can never read it; browsers send it only to the auth endpoints and never on cross-site requests. `register`, `login`, and `refresh` set or rotate it; `logout` clears it (`Max-Age=0`).
- **Access token → memory only** in the web client. On page load the client calls `POST /api/auth/refresh` (the cookie authenticates) to get a new one. Every other endpoint keeps `Authorization: Bearer`, so none of them depend on cookies and they need no CSRF protection.
- **CSRF on the cookie endpoints** (`refresh`, `logout`): `SameSite=Strict`, plus the request must carry an **`X-Requested-With`** header, otherwise `403`. A cross-site page cannot add that header without a CORS preflight, which the API refuses.
- **Topology:** the Next.js server proxies `/api/*` to the backend (`rewrites` in `next.config.ts`, target `API_PROXY_TARGET`), so the browser talks to a single origin. The cookie is first-party and CORS no longer matters for the browser.
- **Client IP behind the proxy.** Verified empirically: Next's rewrite proxy forwards `Cookie`, custom headers, and `Set-Cookie`, and **forwards an incoming `X-Forwarded-For` but does not add one itself**. Therefore `server.forward-headers-strategy` defaults to **`none`** (fail-safe). The API sees the proxy's address, so per-IP rate limits (ADR-011) act as a single global bucket; per-account limits are unaffected. Set `FORWARD_HEADERS_STRATEGY=native` **only** when a load balancer in front of Next overwrites `X-Forwarded-For` with the real client IP. Tomcat then trusts it from internal (private/localhost) proxies. With Next exposed directly, `native` would let clients spoof their IP.
- **Config:** `brewdeck.auth.refresh-cookie.*`; `AUTH_COOKIE_SECURE` (default `true`, `false` in the `local` profile for plain-HTTP development).

## Plan (each step backward-compatible)
1. **Backend dual mode** (this PR): set the cookie, and accept the token from the **cookie or** the legacy JSON body (the cookie wins). The response body still includes `refreshToken`.
2. **Frontend:** Next rewrites; access token in memory; refresh via the cookie on load; **Web Locks** so several tabs don't rotate at the same time (rotation reuse detection would otherwise log the user out); remove the `localStorage` keys; forward-header config (default `none`, see above).
3. **Backend:** stop returning `refreshToken` in JSON and stop accepting it in the body.

## Consequences
- **Positive:** an XSS can no longer steal a long-lived credential. At worst it acts inside the open tab while the 15-minute access token lasts.
- **Positive:** a single origin removes CORS from the browser path entirely.
- **Negative:** each page load does one refresh round-trip (and rotation).
- **Negative:** the frontend must run as a Node server (it already does), not a static export.
- **Negative:** sessions stored in `localStorage` before step 2 end once. Users log in again.

## Alternatives Considered
- **Both tokens in cookies:** every write endpoint would need CSRF protection and the JWT filter would change. That is more surface for little extra gain, since the access token is short-lived.
- **Cross-site cookies (`SameSite=None`):** needed only without the proxy; weaker, and requires CORS with credentials.
- **Keep `localStorage` and rely on CSP:** CSP reduces XSS risk but does not remove it; the stolen-refresh-token impact would stay.
