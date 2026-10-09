# Technical Spike: Free Local LLM for the AI Recipe Assistant

## 1. Summary

The AI recipe assistant ("Suggest with AI" and "Improve with AI") is built but paused because the
owner does not want to pay for an LLM API ([AI suspension note](../../../.claude/project-state.md)).
This spike checks whether a free, open-weight model running locally through Ollama can replace the
paid provider behind the existing `RecipeSuggestionPort` (ADR-006).

- **Main question:** which local model, if any, gives usable suggestions and improvements on the
  owner's Mac, and how should BrewDeck call it?
- **Expected decision:** model, integration approach (Spring AI or a plain HTTP call), and the
  guardrails needed before re-enabling `brew-recipe-ai-assistant` in `local`.

## 2. Background

Facts from the repository (2026-10-08):

- `RecipeSuggestionPort` has `suggest(SuggestionContext)` and `improve(ImprovementContext)`.
  Adapters: `ClaudeRecipeSuggestionAdapter` (`brewdeck.ai.enabled=true`) and
  `DisabledRecipeSuggestionAdapter` (`false`).
- Endpoints `POST /api/recipes/suggest` and `POST /api/recipes/{id}/improve` (422 without rated
  history) exist, are authenticated, and are gated by the `brew-recipe-ai-assistant` flag. The web
  app has both buttons.
- The suggestion contains coffee grams, water grams, ratio, grind setting, water temperature,
  brew time, steps, and rationale. The context contains coffee, origin, roast, process, four
  tasting scores, the method, and user notes. "Improve" adds the current recipe and its rated
  brews.
- Stack: Spring Boot 4.1 on Jackson 2 through the `spring-boot-jackson2` shim (ADR-008).
- Machine: Apple M4, 24 GB RAM.

A ChatGPT proposal the owner shared (not kept in the repo) suggested a new module with
`POST /api/v1/ai/recipes/suggest`, Spring AI, Ollama in Docker Compose, and `llama3.2:3b`. This
spike tested those suggestions against the code and the hardware.

## 3. Goals

- Measure 2–3 local models on fixed scenarios: valid structured output, sensible values, Spanish
  text, and latency.
- Decide how BrewDeck should call Ollama.
- List the guardrails a local model needs.

## 4. Non-Goals

- No production hosting decision (that needs a server with RAM or a GPU, which costs money).
- No chat assistant, RAG, or fine-tuning.
- No code changes in this spike; the adapter is the next PR.

## 5. Key Questions

- Q-001: Which model gives the best mix of correctness, Spanish, and latency on this Mac?
- Q-002: Spring AI or a plain HTTP call to Ollama?
- Q-003: Ollama natively or in Docker?
- Q-004: What must BrewDeck validate itself?

## 6. Options Considered

| Model | License | Size (Q4) | Notes |
| --- | --- | --- | --- |
| `llama3.2:3b` | Llama Community License (not OSI) | 2.0 GB | ChatGPT's suggestion |
| `llama3.1:8b` | Llama Community License (not OSI) | 4.9 GB | ChatGPT's alternative |
| `qwen3:8b` | Apache 2.0 | 5.2 GB | Claude's suggestion; run with thinking off |

## 7. Method

- Ollama 0.40.1 installed natively (`brew install ollama`), using the Apple GPU.
- Script: [`ai-local-llm/evaluate.py`](ai-local-llm/evaluate.py). It uses the Claude adapter's
  exact system prompts and user-message layout, adds "Write steps and rationale in Spanish", and
  requests the same eight fields through Ollama's JSON-schema `format`. Temperature 0.3, at most
  1024 output tokens.
- 8 scenarios: 6 "suggest" (V60, AeroPress, espresso, French press, Chemex, cold brew) and
  2 "improve" (a sour V60 history and a bitter French press history). They are synthetic but
  realistic Mexican coffees: **the owner's local database had no coffees or recipes** when the
  spike ran (only the 10 seeded methods).
- Each answer is scored for schema validity, a water-to-coffee ratio inside the method's usual
  range, the 70–100 °C temperature rule, the stated ratio matching the grams, and Spanish text.
  The improve answers were also read by hand to check the direction of each change.
- Raw answers: [`ai-local-llm/results-2026-10-08.json`](ai-local-llm/results-2026-10-08.json).

## 8. Results

| Model | Valid JSON | Answers with no flagged issue | Median latency | Speed |
| --- | --- | --- | --- | --- |
| `llama3.2:3b` | 7 / 8 | 4 / 8 | 6.3 s | 36 tok/s |
| `llama3.1:8b` | 8 / 8 | 5 / 8 | 15.5 s | 14 tok/s |
| `qwen3:8b` | 8 / 8 | 4 / 8 | 20.4 s | 13 tok/s |

All three wrote steps and rationale in Spanish, with occasional English leaking into short fields
("Medium-coarse", "How to adjust grind:").

### Finding 1: `llama3.2:3b` is fast but not usable

- Garbage in string fields (ratio `":["`, `"F:1-16.7"`), steps returned as a JSON string inside
  `steps`, an espresso with 280 g of water, and one runaway answer that hit the 1024-token cap
  without closing the JSON.
- **Impact:** it fails ChatGPT's own acceptance criterion of a validated structured response.

### Finding 2: The 8B models get "improve" right and "suggest" ratios wrong

- **Improve (the sour V60):** both moved the grind finer. Qwen also raised the temperature
  (92 → 93 °C) and explained the change correctly. Llama raised it to 95 °C but wrote that it
  "lowered" it.
- **Improve (the bitter French press):** both moved to a coarser grind and a slightly lower
  temperature. Those are the right directions.
- **Suggest:** each 8B model got the ratio clearly wrong for 2–3 methods. Examples: V60 at 1:9
  (Qwen) or 1:11 (Llama), Chemex at 1:40 (Qwen), and a cold brew Llama did not know how to
  express (water/coffee 666).
- Qwen nailed espresso (18 g in, 45 g out, 92 °C, 25–30 s). Llama put 14 g in and 28 g out.
- **Impact:** a local 8B model is good at adjusting a recipe that already exists, and unreliable
  at inventing numbers from scratch.

### Finding 3: Local models need a hard output cap

`llama3.1:8b` once generated more than 4,600 tokens without closing the JSON until the request
timed out. With `num_predict` capped, the same failure showed up as a clean "truncated" result.

**Impact:** the adapter must cap tokens and treat `done_reason: "length"` as a failure.

### Finding 4: Latency is acceptable for a button, not for typing

15–25 s per call for the 8B models on the M4.

**Impact:** the backend timeout must be at least 60 s, the UI needs a clear "thinking" state, and
the endpoints need a per-user rate limit (today only auth endpoints are limited, ADR-011),
because the cost is now CPU and GPU time, not money.

### Finding 5: Spring AI 2 brings Jackson 3

Spring AI 2.0 (GA 2026-06-12) supports Boot 4, but `spring-ai-ollama` and `spring-ai-model`
depend on `tools.jackson.core:jackson-databind` (Jackson 3). BrewDeck runs on Jackson 2 through
the compatibility shim (ADR-008), and Spring AI's auto-configuration expects Boot's Jackson 3 setup.

**Impact:** adopting Spring AI first needs the deferred Jackson 3 migration. A single
`RestClient` call to Ollama's `/api/chat` (JSON-schema `format`, `num_predict`, `think: false`)
needs no new dependency.

### Finding 6: Docker on macOS cannot use the Apple GPU

Ollama in Docker on a Mac runs on the CPU only, which is several times slower than the native
install measured here.

**Impact:** use Docker only on a Linux server.

### Finding 7 (product bug, not AI): Cold Brew cannot be saved

- Cold Brew is a seeded method (V2), but `RecipeRequest.waterTemp` and
  `BrewSessionRequest.actualTemp` (and the matching Zod schemas) require 70–100 °C.
- The AI prompt repeats the rule. Qwen answered with the physically correct 15 °C, which the app
  would reject.

**Impact:** temperature limits should depend on the method (or allow a cold range). This is a
separate fix.

## 9. Trade-Off Analysis

| Trade-off | Benefit | Cost |
| --- | --- | --- |
| Local 8B model instead of a paid API | Free, private, works offline | 15–25 s latency; weaker numbers; needs guardrails |
| Plain HTTP instead of Spring AI | No Jackson 3 dependency, small adapter | No provider abstraction beyond the existing port (which already provides one) |
| Qwen3 8B over Llama 3.1 8B | Apache 2.0; coherent "improve" rationales; best espresso | About 5 s slower; similar ratio errors |

## 10. Risks

| Risk | Impact | Likelihood | Mitigation |
| --- | --- | --- | --- |
| Wrong ratios or temperatures reach the form | Bad recipe | High (Finding 2) | Deterministic method ranges: validate, clamp or retry once, and show it as a suggestion the user edits before saving (the form already does this) |
| Runaway generation | Hung request | Medium | `num_predict` cap, 60 s timeout, treat truncation as `AiUnavailableException` |
| Slow responses tie up the machine | Poor UX, CPU load | Medium | Per-user rate limit on the AI endpoints; one request at a time |
| English leaking into Spanish | Mixed-language UI | Medium | Prompt in the user's language, using the request locale (ADR-015) |
| Llama license terms | Legal | Low for a personal project | Prefer the Apache-2.0 model |

## 11. Recommendation

**Recommended option: `qwen3:8b` on native Ollama, behind a new `OllamaRecipeSuggestionAdapter`
that calls `/api/chat` with a plain `RestClient`, plus deterministic guardrails.**

- It keeps the existing endpoints, port, flag, and buttons. There is no parallel module and no
  invented fields such as grinder or flavor preference.
- Qwen's "improve" answers moved in the right direction with a consistent rationale. Its
  "suggest" numbers need the same server-side checks that any local model would.
- Llama 3.1 8B is a close second (faster, but its rationale contradicted its own numbers once).
  Llama 3.2 3B is ruled out.
- Spring AI waits for the Jackson 3 migration.

Validate next, in the adapter PR:

- **Method-aware ranges.** Ratio and temperature per brew method, enforced after the model
  answers, with one retry and then a clamp.
- **Provenance.** Store the provider, model, and prompt version with each accepted suggestion
  (taken from ChatGPT's proposal).
- **Locale.** Pass the request locale into the prompt.
- **Optional:** try `qwen3:14b`, which fits in 24 GB but is slower, if the 8B quality is not enough.

## 12. Decision

- Decision: `qwen3:8b` on native Ollama behind `OllamaRecipeSuggestionAdapter`, with method-aware guardrails, a per-user rate limit, and plain HTTP instead of Spring AI.
- Date: 2026-10-08
- Owner: Juan (product owner)
- Status: Accepted. Recorded in [ADR-016](../../decisions/ADR-016-local-llm-ollama.md); delivered on `feat/api-ollama-adapter`.

## 13. Next Steps

1. Owner decides on the recommendation.
2. Adapter PR (`feat(api)`):
   - `OllamaRecipeSuggestionAdapter` selected by `brewdeck.ai.provider=ollama`.
   - Settings `OLLAMA_BASE_URL` and `OLLAMA_MODEL`, a 60 s timeout, and a `num_predict` cap.
   - Method-aware validation, a per-user rate limit, and provenance fields.
   - Unit tests with a stubbed Ollama, plus a manual run on the real model.
3. Re-enable `brew-recipe-ai-assistant` in `local` only, and try it in the app.
4. ~~Separate fix for Cold Brew (Finding 7).~~ Done: brew temperatures accept 0–100 °C (`fix/cold-brew-temperature`).
5. Later, only if needed: the Jackson 3 migration, then Spring AI; production hosting; the
   chat assistant and RAG.

## 14. ADR Candidate

- ADR needed: Yes, once decided.
- Suggested title: "ADR-016: Local open-weight LLM through Ollama for the AI recipe assistant".
- Reason: it changes the AI provider strategy recorded in ADR-006 and adds a runtime dependency
  (an Ollama server).

## 15. Open Questions

- OQ-001: Should "suggest" keep calling the model for every number, or start from deterministic
  method defaults (ratio, temperature, time) and let the model adjust and explain? Finding 2
  favors the second option.
- OQ-002: Should the AI answer follow the user's language (ADR-015), or always be Spanish?

## 16. Assumptions

- Assumption-001: Synthetic scenarios represent the owner's coffees. Re-run the script once the
  local database has real recipes.
- Assumption-002: One run per scenario at temperature 0.3 is enough to compare models. A larger
  sample would tighten the numbers.
