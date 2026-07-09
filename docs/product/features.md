# Features

Current and planned features. Deep design detail for each shipped feature lives in
[`docs/superpowers/specs/`](../superpowers/specs/); lightweight FDDs for a few
notable features live in [`fdd/`](fdd/).

## Shipped

### Core domain
- **Coffees** — CRUD with origin/process/roast/notes and numeric tasting scores (acidity, body, sweetness, bitterness).
- **Brew methods** — CRUD; seeded with initial methods.
- **Recipes** — CRUD; grind, ratio, temperature, time, steps, expected taste; favorites.
- **Brew sessions** — CRUD; actuals + rating + adjustment notes, linked to a recipe.

### Discovery & UX
- **Favorites** — favorite/unfavorite recipes + dedicated favorites view.
- **Filtering** — Specification-based filters for coffees, recipes, brew sessions.
- **Pagination** — all collection endpoints return a `PageResponse<T>` envelope.
- **Dashboard summary** — aggregate counts and highlights.

### Analytics (read-only)
- Recipe stats, top-rated recipes, most-brewed recipes.
- Most-used coffees, brew-method usage breakdown.
- Rating trend over time (recipe detail chart).
- Coffee tasting-notes radar (recipe/coffee detail).

### Intelligence & sharing
- **AI recipe suggestions** — feature-flagged `POST /api/recipes/suggest` (Claude via a hexagonal port). See [FDD](fdd/ai-recipe-suggestions-fdd.md).
- **AI recipe improve** — feature-flagged `POST /api/recipes/{id}/improve` tuned from rated session history.
- **Recipe PDF export** — client-side one-page recipe card (jsPDF).
- **Public share links** — opt-in revocable token + public read-only page. See [FDD](fdd/public-share-links-fdd.md).

### Platform
- **Auth foundation** — self-registration, JWT login, stateless gate on `/api/**`.
- CORS, health probe (`/actuator/health`), OpenAPI/Swagger, structured logs.

## Planned

- Per-user ownership (Slice B) and account UX (Slice C).
- Cloud deployment.
- E-paper companion device + offline sync (vision).
