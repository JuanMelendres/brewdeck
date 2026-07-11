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
POST  /api/auth/register          201
POST  /api/auth/login             200
GET   /api/auth/me                200 (401 without token)
PATCH /api/auth/me                200 (update display name)
POST  /api/auth/change-password   204 (400 if current password wrong)
```

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
GET    /api/brew-methods
GET    /api/brew-methods/{id}
POST   /api/brew-methods
PUT    /api/brew-methods/{id}
DELETE /api/brew-methods/{id}
GET    /api/brew-methods/usage           (analytics, List)
```

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
