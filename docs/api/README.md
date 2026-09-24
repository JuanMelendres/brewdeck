# API Reference

Endpoint catalog for the BrewDeck REST API. Design principles and conventions are
in [`architecture/api-design.md`](../architecture/api-design.md).

- **Base URL (local):** `http://localhost:8080`
- **Live docs:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI seed:** [`openapi.yaml`](openapi.yaml)
- **Postman:** [`postman/brewdeck.postman_collection.json`](postman/brewdeck.postman_collection.json) + [`brewdeck.local.postman_environment.json`](postman/brewdeck.local.postman_environment.json)

> Collection GETs are paginated: `?page=0&size=10&sort=id,asc` and return `PageResponse<T>`.

## Auth (`/api/auth`)
```
POST  /api/auth/register            201
POST  /api/auth/login               200
GET   /api/auth/me                  200 (401 without token; includes emailVerified and role: USER|ADMIN)
PATCH /api/auth/me                  200 (update display name)
POST  /api/auth/change-password     204 (400 if current password wrong; revokes all refresh tokens)
POST  /api/auth/forgot-password     200 (always; no user enumeration)
POST  /api/auth/reset-password      204 (400 if token invalid/expired/used; revokes all refresh tokens)
POST  /api/auth/verify-email        204 (400 if token invalid/expired/used)
POST  /api/auth/resend-verification 200 (authenticated; no-op if already verified)
POST  /api/auth/refresh             200 (public; 401 if refresh token invalid/expired/used; 400 if blank)
POST  /api/auth/logout              204 (authenticated; revokes only the presented refresh token)
```

> Emails are case-insensitive. `register`, `login`, and `forgot-password` trim and lowercase the
> address, and responses return that normalized form. Registering `Juan@x.com` when
> `juan@x.com` exists is a `409`.

> Changing or resetting a password ends every existing session: all of the user's active refresh
> tokens are revoked, so other devices must log in again. Already-issued access tokens stay valid
> until they expire (`AUTH_TOKEN_TTL`, default 15 minutes).

> `register` and `login` responses now also include a `refreshToken` field alongside `token`, `expiresAt`, and `email`.

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
POST   /api/recipes/suggest              (AI, feature-flagged)
POST   /api/recipes/{id}/improve         (AI, feature-flagged)
PUT    /api/recipes/{id}
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

## Public (no auth) (`/api/public`)
```
GET    /api/public/recipes/{token}
```

## Dashboard & system
```
GET    /api/dashboard/summary
GET    /actuator/health
```

> Keep this catalog and [`openapi.yaml`](openapi.yaml) in sync with the controllers. When endpoints change, also run the `update-postman` workflow.
