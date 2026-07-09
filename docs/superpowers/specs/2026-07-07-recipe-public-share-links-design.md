# Recipe Public Share Links — Design Spec

**Date:** 2026-07-07
**Status:** Approved (design)
**Slice:** Phase 5 product improvement. Full-stack. Last open Phase 5 feature — closes the phase. Follows the recipe PDF export slice (PR #61 merged).

## Goal

Let a user create an unguessable, read-only public link to a single recipe and
revoke it later. Anyone with the link opens a standalone page showing the recipe
card — no app account or navigation required. Sharing is opt-in per recipe and
revocable.

## Context

The BrewDeck API has **no Spring Security on the classpath** — every `/api/**`
endpoint is already unauthenticated and open. This slice therefore does not
pierce an existing auth wall (there is none). The real concerns are:

1. **Non-enumerability** — a share URL must carry an unguessable token, never the
   sequential numeric recipe id, so recipes cannot be walked by incrementing an
   integer.
2. **Curated exposure** — the public endpoint returns a purpose-built DTO with
   only the recipe-card fields, never internal ids, the personal `favorite`
   flag, or timestamps.

## Non-Goals

- No authentication/authorization system, no login, no per-user ownership
  (the app has no users today; not introducing them here).
- No share analytics (view counts), no link expiry/TTL, no password-protected
  links, no bulk/multi-recipe sharing, no social/OG preview image generation.
- No editing or any write action from the public page — read-only.
- No change to existing recipe CRUD endpoints beyond adding `shareToken` to the
  existing `RecipeResponse`.

## Decisions (from brainstorming)

1. **Share model:** opt-in, revocable. Recipe is private (no token) by default.
   Share generates a token; unshare clears it; a public fetch works only while a
   token exists. Re-sharing is idempotent (returns the existing token).
2. **Token format:** secure-random, URL-safe. 16 bytes from `SecureRandom`
   encoded base64url without padding (~22 chars), e.g.
   `/share/Xk7pQ2mN8vL4wR9tYbZ0aQ`. No external dependency.
3. **Public fields:** card content only — `name`, `coffeeName`, `methodName`, the
   eight brew params, `steps`, `expectedTaste`. No ids, no `favorite`, no
   timestamps.
4. **Public page rendering:** client-side, matching the project pattern —
   TanStack Query + `apiFetch`, with the standard loading / error / empty states.

## Architecture

### Backend (`recipe` domain)

- **`V4__recipe_share_token.sql`** (new migration) — add nullable
  `share_token VARCHAR(32)` to `recipes`; add a unique index on `share_token`
  (partial: `WHERE share_token IS NOT NULL`, so many null rows coexist). Existing
  rows keep `share_token = NULL` (private).
- **`Recipe`** (entity, modified) — add
  `@Column(name = "share_token", unique = true) private String shareToken;`.
- **`RecipeRepository`** (modified) — add
  `Optional<Recipe> findByShareToken(String shareToken);`.
- **`RecipeService`** (modified) — add:
  - `RecipeResponse share(Long id)` — load recipe (404 if missing); if
    `shareToken == null`, generate one and save; return `RecipeResponse`
    (idempotent — an already-shared recipe keeps its token).
  - `RecipeResponse unshare(Long id)` — load recipe (404 if missing); set
    `shareToken = null`, save; return `RecipeResponse` (no-op-safe if already
    unshared).
  - `PublicRecipeResponse getByShareToken(String token)` — `findByShareToken`
    (404 if absent); map to `PublicRecipeResponse`.
  - Private helper `generateToken()` — 16 secure-random bytes,
    `Base64.getUrlEncoder().withoutPadding().encodeToString(...)`; a single
    static `SecureRandom` instance.
- **`PublicRecipeResponse`** (new record) — `name`, `coffeeName`, `methodName`,
  `coffeeGrams` (BigDecimal), `waterGrams` (BigDecimal), `ratio`,
  `grindSetting`, `waterTemp` (Integer), `brewTime`, `steps`, `expectedTaste`;
  with `fromEntity(Recipe)`. No id/favorite/timestamps.
- **`RecipeResponse`** (modified) — add `String shareToken` field (mapped in
  `fromEntity`), so the private detail GET tells the app whether the recipe is
  shared and lets the client build the link. Exposing it on the already-open
  private endpoint leaks nothing new.
- **`RecipeController`** (modified) — `PATCH /api/recipes/{id}/share`,
  `PATCH /api/recipes/{id}/unshare`, both returning `RecipeResponse` (200),
  mirroring the existing favorite/unfavorite PATCH pair.
- **`PublicRecipeController`** (new) — `GET /api/public/recipes/{token}` returns
  `PublicRecipeResponse` (200) or 404 (unknown/revoked token) via the existing
  `GlobalExceptionHandler`.

**CORS:** the existing `WebConfig` maps `/api/**`; `/api/public/**` is already
covered — no change. The `/api/public/` prefix documents intent; there is no auth
to bypass.

### Frontend (`recipes` feature)

- **`src/lib/api/types.ts`** (modified) — `Recipe` gains
  `shareToken: string | null`; new `PublicRecipe` type (card fields only).
- **`src/lib/api/recipes.ts`** (modified) — `shareRecipe(id): Promise<Recipe>`
  (`PATCH /api/recipes/{id}/share`), `unshareRecipe(id): Promise<Recipe>`
  (`PATCH /api/recipes/{id}/unshare`).
- **`src/lib/api/publicRecipes.ts`** (new) — `getPublicRecipe(token):
  Promise<PublicRecipe>` (`GET /api/public/recipes/{token}`).
- **Hooks** — `useShareRecipe` / `useUnshareRecipe` (`useMutation`; `onSuccess`
  invalidates the `recipe(id)` query key); `usePublicRecipe(token)`
  (`useQuery`).
- **`ShareRecipeDialog`** (new, in `src/components/recipes/`) — driven by
  `recipe.shareToken`:
  - Not shared: explanatory text + **Create link** button (`useShareRecipe`).
  - Shared: read-only field showing `${window.location.origin}/share/${token}`,
    a **Copy** button (`navigator.clipboard.writeText`), and a **Stop sharing**
    button (`useUnshareRecipe`).
- **`RecipeDetailView`** (modified) — a **Share** button beside Export PDF opens
  the dialog.
- **`src/app/share/[token]/page.tsx`** (new; `export default`, required for a
  Next.js page) — reads `params.token`, renders `PublicRecipeView`.
- **`PublicRecipeView`** (new client component) — `usePublicRecipe(token)`;
  renders a read-only branded recipe card; standalone (no app nav). Loading →
  `Spinner`; error/404 → empty state "This recipe isn't available"; success →
  the card.

### Data flow

```
Owner (recipe detail)
  └─ Share button → ShareRecipeDialog
       ├─ Create link → PATCH /api/recipes/{id}/share → token → invalidate recipe(id)
       ├─ Copy → navigator.clipboard.writeText(`${origin}/share/${token}`)
       └─ Stop sharing → PATCH /api/recipes/{id}/unshare → token cleared → invalidate

Viewer (anyone with the link)
  └─ /share/{token} → PublicRecipeView → GET /api/public/recipes/{token}
       ├─ 200 PublicRecipeResponse → read-only card
       └─ 404 (unknown / revoked) → "This recipe isn't available"
```

## Error handling

- **Unknown / revoked token:** `GET /api/public/recipes/{token}` →
  `NotFoundException` → `GlobalExceptionHandler` 404. Frontend maps the query
  error to the "This recipe isn't available" empty state (not `console.error`).
- **Share / unshare on a missing recipe id:** 404 (existing not-found pattern).
- **Idempotency:** re-sharing returns the existing token (no new token);
  unsharing an already-unshared recipe is a safe no-op returning 200.
- **Clipboard failure:** the dialog shows an inline Alert
  ("Couldn't copy — copy it manually"); the link text stays visible/selectable.

## Testing

### Backend
- **Service:** token generated on first share, unique, idempotent on re-share;
  unshare clears the token; `getByShareToken` returns the mapped DTO and throws
  not-found for an unknown token.
- **Controller (MockMvc):** share → 200 with a non-null `shareToken`; unshare →
  200 with `shareToken` null; public GET → 200 with the curated body **and an
  assertion that the JSON has no `id`, `favorite`, or `createdAt`**; public GET
  unknown token → 404; public GET after unshare → 404.
- **Repository:** `findByShareToken` returns the row when present, empty when not.
- **Integration (Testcontainers):** full flow — create recipe, share, fetch by
  token (200), unshare, fetch again (404). Control the dataset; do not assume a
  single row.

### Frontend
- **`publicRecipes` api:** calls the right URL, parses the body.
- **`useShareRecipe` / `useUnshareRecipe`:** invalidate `recipe(id)` on success
  (wrap hooks in `QueryClientProvider`).
- **`ShareRecipeDialog`:** renders the not-shared state (Create link) and the
  shared state (link text, Copy, Stop sharing); clicking Create calls
  `shareRecipe`; clicking Stop sharing calls `unshareRecipe`; Copy calls the
  clipboard (mock `navigator.clipboard`).
- **`PublicRecipeView`:** loading, 404 empty state, and success card.
- **`RecipeDetailView`:** the Share button renders and opens the dialog.
- Run the **full** `vitest run` — the Share button is added to the shared
  `RecipeDetailView` (sibling tests mount it; see the sibling-test regression in
  project memory).

## Task decomposition (for the plan)

1. **Backend** — V4 migration, `Recipe.shareToken`, `findByShareToken`, service
   `share`/`unshare`/`getByShareToken` + token gen, `PublicRecipeResponse`,
   `shareToken` on `RecipeResponse`, share/unshare endpoints,
   `PublicRecipeController`. Full backend test set. `./mvnw clean verify` green.
2. **Frontend API + hooks** — `Recipe.shareToken` + `PublicRecipe` types,
   `shareRecipe`/`unshareRecipe`/`getPublicRecipe`,
   `useShareRecipe`/`useUnshareRecipe`/`usePublicRecipe`, + tests.
3. **Frontend share UI** — `ShareRecipeDialog` + Share button on
   `RecipeDetailView`, + tests (full suite).
4. **Frontend public page** — `/share/[token]` route + `PublicRecipeView`,
   + tests.
5. **Docs** — roadmap (mark Phase 5 Public share links Done / phase complete),
   project-state, Postman (share / unshare / public-fetch requests).

## Global constraints

- **Backend:** organize by domain (`recipe`); never return entities from
  controllers — map to records; Bean Validation on inputs; no special symbols
  like `°C` in validation messages (write "degrees Celsius"); JaCoCo/Sonar DTO
  exclusion parity where applicable.
- **Frontend:** strict TypeScript, no `any`; import domain types from
  `@/lib/api`; named exports everywhere except Next.js `page.tsx` (which uses
  `export default`); absolute `@/` imports; TanStack Query for all server state
  with correct key invalidation; handle loading / error / empty visibly (no
  `console.error`).
- Response shape: `GET /api/public/recipes/{token}` returns the DTO directly
  (single resource — not a `PageResponse`).
- **Commands:** frontend from `brewdeck-web/` (`npm run test`, `type-check`,
  `build`; scope `lint:fix` to changed files); backend `./mvnw spotless:apply`
  then `./mvnw clean verify`.
- Conventional Commits; scopes `api` (backend), `web` (frontend), `docs`.
