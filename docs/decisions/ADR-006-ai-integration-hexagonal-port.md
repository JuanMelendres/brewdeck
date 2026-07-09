# ADR-006: AI integration behind a hexagonal port

## Status
Accepted

## Date
2026-07-09

## Context
BrewDeck adds AI-assisted recipe suggestions and improvements. The LLM provider (Anthropic Claude) is an external, paid, optional dependency that must not couple to core business logic, must be toggleable, and must be excluded from tests by default.

## Decision
Access the LLM through a **`RecipeSuggestionPort`** (hexagonal architecture) with a Claude adapter (`claude-haiku-4-5`, structured outputs). The feature is gated by `AI_ENABLED` (default off) and `ANTHROPIC_API_KEY` from the environment. Suggestions are ephemeral — returned to pre-fill the form, never auto-persisted.

## Consequences
- **Positive:** core services depend on an interface, not a vendor SDK; the adapter is swappable.
- **Positive:** feature ships dark (flag off), safe for CI and offline dev.
- **Negative:** live SDK path is untested by design (feature off by default); adapter is Sonar-excluded.
- **Negative:** structured-output parsing must handle malformed responses (mapped to `503`).

## Alternatives Considered
- **Direct SDK calls in the service** — rejected: vendor coupling, hard to test.
- **Persisting AI output** — rejected: keep suggestions ephemeral until the user saves.

## Notes
`improve` requires rated brew history (`422` otherwise). Revisit provider/model choice and rate limiting when multi-user ownership lands.
