# Feature: Public Share Links

Design detail: [`docs/superpowers/specs/2026-07-07-recipe-public-share-links-design.md`](../../superpowers/specs/2026-07-07-recipe-public-share-links-design.md).

## Summary

A recipe owner can generate an opt-in, revocable share token. Anyone with the resulting link can view a curated, read-only version of the recipe without authenticating.

## Problem

Users want to share a recipe with a friend without giving access to the app or exposing internal fields (ids, favorite flag, timestamps).

## Users

- **Owner:** authenticated user who shares/unshares a recipe.
- **Visitor:** anonymous person opening a share link.

## Functional Requirements

- FR-001: `PATCH /api/recipes/{id}/share` generates and stores a share token.
- FR-002: `PATCH /api/recipes/{id}/unshare` clears the token (revokes access).
- FR-003: `GET /api/public/recipes/{token}` returns a curated `PublicRecipeResponse` (no id, favorite, or timestamps) and requires no auth.
- FR-004: The frontend exposes a Share dialog and a standalone public `/share/[token]` page.

## Business Rules

- BR-001: Sharing is opt-in and off by default (token is null until shared).
- BR-002: Tokens are unique (enforced by a partial unique index on non-null tokens).
- BR-003: Unsharing invalidates the link immediately.
- BR-004: The public endpoint stays open even while all other `/api/**` routes are auth-gated.

## User Flow

1. Owner opens a recipe and clicks Share.
2. API generates a base64url token; UI shows the public link.
3. Visitor opens `/share/{token}` → public page fetches `GET /api/public/recipes/{token}`.
4. Owner clicks Unshare → token cleared → link 404s.

## Edge Cases

- Case 1: Visitor opens a revoked/unknown token → `404` / not-found page.
- Case 2: Owner re-shares → a new token is generated (`Assumption`: confirm whether the old token is reused or replaced).

## Out of Scope

- Expiring / time-boxed links.
- Password-protected links.
- Public listing or discovery of shared recipes.

## Open Questions

- OQ-001: Should share links expire automatically? `TODO`.
- OQ-002: Should the owner see view counts? `TODO`.
