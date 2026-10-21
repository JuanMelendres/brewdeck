# API Reference

Endpoint catalog for the BrewDeck REST API. Design principles and conventions are
in [`architecture/api-design.md`](../architecture/api-design.md).

- **Base URL (local):** `http://localhost:8080`
- **Live docs:** `http://localhost:8080/swagger-ui/index.html` (local/dev only; disabled in the `prod` profile unless `API_DOCS_ENABLED=true`)
- **OpenAPI seed:** [`openapi.yaml`](openapi.yaml)
- **Postman:** [`postman/brewdeck.postman_collection.json`](postman/brewdeck.postman_collection.json) + [`brewdeck.local.postman_environment.json`](postman/brewdeck.local.postman_environment.json)

> Collection GETs are paginated: `?page=0&size=10&sort=id,asc` and return `PageResponse<T>`.

## Auth (`/api/auth`)
```
POST  /api/auth/register            202 (always, same body; no session; new email → verification email, existing → account-exists email; while legal-terms is on, 400 validationErrors.acceptedTermsVersion unless it equals the current version, which is stored on the new user)
POST  /api/auth/login               200
GET   /api/auth/me                  200 (401 without token; includes emailVerified, role: USER|ADMIN, themePreference: LIGHT|DARK|null, language: EN|ES|null)
PATCH /api/auth/me                  200 (update display name)
PUT   /api/auth/me/theme            200 (set themePreference LIGHT|DARK; 400 if missing or unknown)
PUT   /api/auth/me/language         200 (set language EN|ES; 400 if missing or unknown)
POST  /api/auth/change-password     200 (new access token + refresh cookie for the caller; 400 if current password wrong; ends every other session)
POST  /api/auth/forgot-password     200 (always; no user enumeration)
POST  /api/auth/reset-password      204 (400 if token invalid/expired/used; ends every session)
POST  /api/auth/verify-email        204 (400 if token invalid/expired/used)
GET   /api/auth/me/export           200 JSON attachment: profile + own coffees, brew methods, recipes, brew sessions; no hashes; 429 over 5/h (ADR-020)
DELETE /api/auth/me                 204 {currentPassword}; deletes the account and all its data, ends every session, clears the refresh cookie, emails a confirmation; 400 wrong password; 429 over 5/15 min (ADR-020)
POST  /api/auth/me/email-change     202 (always, same body, whether or not the new address has an account; 400 wrong current password or same email; 429 over 3/h; 404 while auth-email-change is off; ADR-019)
POST  /api/auth/email-change/confirm 204 (public; 400 if link invalid/expired/used/cancelled or address taken meanwhile; new email verified; ends every session; 404 while the flag is off)
POST  /api/auth/email-change/cancel  204 (public; 400 if link invalid/expired/used; ends every session; never behind the flag)
POST  /api/auth/resend-verification 200 (authenticated; no-op if already verified)
POST  /api/auth/refresh             200 (public; refresh token from the httpOnly cookie + X-Requested-With header; 401 if invalid/expired/used/missing; 403 cookie without header)
POST  /api/auth/logout              204 (authenticated; revokes the presented refresh token, invalidates outstanding access tokens, clears the cookie)
```

> Public auth endpoints are rate limited per client IP, and login and forgot-password also per
> account. Over the limit you get `429` with `Retry-After` (seconds) and a message such as
> `"Too many attempts. Try again in 3 minutes."`. See
> [ADR-011](../decisions/ADR-011-in-memory-auth-rate-limiting.md) for the limits.

> Error and validation messages follow `Accept-Language`: Spanish (`es`, `es-MX`, ...) or English.
> Any other language, or no header, gets English (ADR-015). Keys live in
> `brewdeck-api/src/main/resources/messages*.properties`.

> Emails are case-insensitive. `register`, `login`, and `forgot-password` trim and lowercase the
> address, and responses return that normalized form.

> **No account enumeration at signup (AUTH-05.7).** `register` answers `202` with the same message
> whether or not the email exists, creates no session, and returns no token or cookie. A new address
> gets the verification email; an existing one gets a "you already have an account" email pointing
> to log in or reset the password. A per-email limit (`REGISTER_EMAIL`, 3 per hour) applies to both,
> so nobody can flood a mailbox. The user logs in after signup.

> Changing or resetting a password ends every existing session: all of the user's active refresh
> tokens are revoked and already-issued access tokens stop working at once, so other devices must
> log in again. `change-password` hands the caller a fresh session so that device stays logged in.
> `logout` also invalidates outstanding access tokens; other devices keep their refresh cookie and
> silently get a new access token (access JWTs carry the user's token version, ADR-013).

> **Refresh token cookie (ADR-013).** `login`, `refresh`, and `change-password` set `brewdeck_refresh`
> (`HttpOnly; Secure; SameSite=Strict; Path=/api/auth`). Cookie-authenticated calls must send an
> `X-Requested-With` header. The refresh token is **never** returned in or accepted from a JSON
> body.

> `login`, `refresh`, and `change-password` return `token`, `expiresAt`, and `email`.

## Feature flags (`/api/feature-flags`)
```
GET /api/feature-flags   200 (authenticated) → { "features": { "aiRecipeAssistant": false } }
```

> Returns only client-exposed flags as an `{alias: boolean}` map — no owner/environment/rollout or
> other admin metadata. The backend is the source of truth: a disabled flag-protected operation
> returns `404` (RELEASE/EXPERIMENT/PERMISSION) or `503` (OPERATIONAL/KILL_SWITCH), never `500`.
> See [`development/feature-flags.md`](../development/feature-flags.md).

## Coffees (`/api/coffees`)
```
GET    /api/coffees
GET    /api/coffees/{id}
POST   /api/coffees
PUT    /api/coffees/{id}
DELETE /api/coffees/{id}
GET    /api/coffees/most-used            (analytics, List)
```

## Brew methods (`/api/brew-methods`)
```
GET    /api/brew-methods                 (shared catalog + the caller's private methods)
GET    /api/brew-methods/{id}            (404 for another user's private method)
POST   /api/brew-methods                 201 (creates a PRIVATE method owned by the caller)
PUT    /api/brew-methods/{id}            200 own private only (403 shared, 404 another user's)
DELETE /api/brew-methods/{id}            204 own private only (403 shared, 404 another user's)
GET    /api/brew-methods/usage           (analytics, List; shared + own methods)
```

## Admin: shared brew-method catalog (`/api/admin/brew-methods`, ADMIN only)
```
POST   /api/admin/brew-methods           201 (creates a SHARED method visible to everyone)
PUT    /api/admin/brew-methods/{id}      200 (404 if not in the shared catalog)
DELETE /api/admin/brew-methods/{id}      204 (404 if not in the shared catalog)
```

> Brew methods have two tiers. The **shared catalog** (`"shared": true`) is visible to everyone
> and only admins change it. **Private methods** (`"shared": false`) belong to the user who
> created them. Nobody else can see them, and a recipe can only use a shared method or one of
> the caller's own. Names are unique within the catalog and within each user's methods. A
> regular user calling `/api/admin/**` gets `403` `{"message":"Insufficient permissions"}`, and
> an anonymous caller gets `401`. See [ADR-009](../decisions/ADR-009-role-based-authorization.md)
> and [ADR-010](../decisions/ADR-010-two-tier-brew-methods.md).

## Recipes (`/api/recipes`)
```
GET    /api/recipes
GET    /api/recipes/{id}
GET    /api/recipes/{id}/stats
GET    /api/recipes/favorites
GET    /api/recipes/top-rated            (analytics, List)
GET    /api/recipes/most-brewed          (analytics, List)
GET    /api/recipes/coffee/{coffeeId}
GET    /api/recipes/method/{methodId}
POST   /api/recipes
POST   /api/recipes/suggest              (AI, feature-flagged; 429 over 10 calls/min per user)
POST   /api/recipes/{id}/improve         (AI, feature-flagged; 429 over 10 calls/min per user)
PUT    /api/recipes/{id}
```

Recipe bodies and responses carry optional `aiModel` / `aiPromptVersion`: the AI answer (`suggest`, `improve`) returns them, and the client sends them back when the user saves that answer, so the recipe records which model and prompt produced it. Hand-written recipes leave them `null`.

Numbers (DATA-01, [numeric-fields.md](../architecture/numeric-fields.md)): `coffeeGrams` / `waterGrams` take at most two decimals and `waterTemp` at most one; more is a `400` field error, never a silent rounding. When both grams are set, the stored `ratio` is derived as `1:<water / coffee>` (one decimal, half up) and any `ratio` in the body is ignored; without both grams, `ratio` stays free text.

```
PATCH  /api/recipes/{id}/favorite
PATCH  /api/recipes/{id}/unfavorite
PATCH  /api/recipes/{id}/share
PATCH  /api/recipes/{id}/unshare
DELETE /api/recipes/{id}
```

## Brew sessions (`/api/brew-sessions`)
```
GET    /api/brew-sessions
GET    /api/brew-sessions/{id}
GET    /api/brew-sessions/recipe/{recipeId}
POST   /api/brew-sessions
PUT    /api/brew-sessions/{id}
DELETE /api/brew-sessions/{id}
```

`actualTemp` takes at most one decimal (`NUMERIC(4,1)`); more is a `400` field error.

## Public (no auth) (`/api/public`)
```
GET    /api/public/recipes/{token}
GET    /api/public/legal              200 {termsVersion, published} (LEGAL-01; published follows the legal-terms flag)
```

## Dashboard & system
```
GET    /api/dashboard/summary
GET    /healthz
```

> Keep this catalog and [`openapi.yaml`](openapi.yaml) in sync with the controllers. When endpoints change, also run the `update-postman` workflow.
