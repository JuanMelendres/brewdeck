# Feature: AI Recipe Suggestions

Design detail: [`docs/superpowers/specs/2026-07-06-ai-recipe-suggestions-design.md`](../../superpowers/specs/2026-07-06-ai-recipe-suggestions-design.md)
and [`...-ai-recipe-improve-design.md`](../../superpowers/specs/2026-07-07-ai-recipe-improve-design.md).

## Summary

Two feature-flagged endpoints let users generate a new recipe or improve an existing one using an LLM. Suggestions are ephemeral: the API returns brewing parameters that pre-fill the recipe form; nothing is persisted until the user saves.

## Problem

Dialing in a recipe (grind, ratio, temperature, time) is trial-and-error. New users lack a starting point; experienced users want data-driven tweaks from their own brew history.

## Users

Authenticated brewers creating or refining recipes.

## Functional Requirements

- FR-001: `POST /api/recipes/suggest` returns suggested brewing parameters for a described coffee/method.
- FR-002: `POST /api/recipes/{id}/improve` returns tuned parameters for an existing recipe, derived from its recent rated brew sessions.
- FR-003: Both endpoints are gated by a feature flag (`AI_ENABLED`, default off) and an `ANTHROPIC_API_KEY` from the environment.
- FR-004: Suggestions are not persisted; the client pre-fills the form and the user decides whether to save.
- FR-005: The AI provider is accessed behind a `RecipeSuggestionPort` (hexagonal), so the adapter is swappable and testable.

## Business Rules

- BR-001: When the feature flag is off (or the SDK fails), the API responds `503`.
- BR-002: `improve` requires at least one rated brew session; otherwise respond `422`.
- BR-003: `improve` on a missing recipe responds `404`.
- BR-004: Only structured, validated brewing fields are returned to the client.

## User Flow

1. User opens the recipe form (or a recipe detail page).
2. User clicks "Suggest with AI" (create) or "Improve with AI" (existing recipe).
3. API calls the LLM behind the port and returns structured parameters + rationale.
4. Form pre-fills; user reviews, edits, and saves normally.

## Edge Cases

- Case 1: AI disabled → `503`, UI surfaces a recoverable message.
- Case 2: No rated history for `improve` → `422`, "Improve" button disabled with a tooltip.
- Case 3: LLM returns malformed output → treated as an AI failure (`503`); no partial data shown.

## Out of Scope

- Persisting or auto-saving AI output.
- Fine-tuning or training any model.
- Multi-model routing / provider selection UI.

## Open Questions

- OQ-001: Should suggestions be rate-limited per user once multi-user ownership lands? `TODO`.
- OQ-002: Should rationale text be stored for later review? `TODO`.
