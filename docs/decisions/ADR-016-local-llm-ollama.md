# ADR-016: Local open-weight LLM through Ollama for the AI recipe assistant

## Status
Accepted (2026-10-08). Runs in `local` only for now.

## Date
2026-10-08

## Context
The AI recipe assistant ("Suggest with AI", "Improve with AI") was built on the paid Claude API
behind `RecipeSuggestionPort` (ADR-006), then paused because the owner did not want to pay for an
LLM API. The [local LLM spike](../product/spikes/ai-local-llm-spike.md) compared free open-weight
models on Ollama, running natively on an Apple M4 with 24 GB of RAM:

- `llama3.2:3b` was not usable.
- The 8B models got "improve" directions right but some "suggest" ratios wrong.
- Answers took 15–25 s.
- Models sometimes generated without end.

## Decision
- **Provider:** a new `OllamaRecipeSuggestionAdapter` implements the existing port. It is selected
  with `brewdeck.ai.provider=ollama` (`AI_PROVIDER`), next to the unchanged Claude adapter
  (`provider=claude`). Endpoints, the `brew-recipe-ai-assistant` flag, and the frontend buttons
  do not change.
- **Model:** `qwen3:8b` by default (`OLLAMA_MODEL`). Apache 2.0 license; Llama models use Meta's
  community license, which is not OSI open source.
- **Transport:** a plain `RestClient` call to Ollama's `/api/chat`, with a JSON-schema `format`,
  `think: false`, a `num_predict` cap, and a 90 s read timeout. Spring AI 2 is not used yet,
  because it depends on Jackson 3 and BrewDeck still runs on Jackson 2 (ADR-008).
- **Failure handling:** a truncated answer (`done_reason: "length"`), invalid JSON, or an Ollama
  error becomes `AiUnavailableException` (503).
- **Guardrails, for every provider:** `SuggestionGuardrails` keeps the water-to-coffee ratio and
  the temperature in the usual range of the brew method, recognized by name. It rewrites the ratio
  as `1:x` from the grams, trims texts to the recipe form's limits, and notes any adjustment in the
  rationale.
- **Prompts:** shared by both adapters (`RecipePrompts`) and versioned. Steps and rationale follow
  the request's language (ADR-015). The model and prompt version are logged with each suggestion.
- **Abuse limit:** 10 AI calls per minute per user (`RateLimitRule.AI_ASSISTANT_USER`). A local
  model costs CPU time instead of money.
- **Rollout:** V26 turns `brew-recipe-ai-assistant` on in `local` only, where the profile defaults
  to `provider=ollama`. Ollama runs natively on macOS, since Docker cannot use the Apple GPU. On a
  Linux server or home lab, the Ollama container is fine.

## Consequences
- **Positive:** the AI assistant costs nothing and keeps data on the machine. Switching back to
  Claude is one setting.
- **Positive:** guardrails catch the numeric mistakes local models make. Claude benefits too.
- **Negative:** answers take 15–25 s, and quality is below a frontier model.
- **Update 2026-10-09 (method hints):** the prompt now gives the model each method's usual dose,
  ratio, temperature, and grind (`BrewMethodProfile`, shared with the guardrails), and asks for every
  text field in the user's language. The guardrails also bound the dose and rewrite the amounts in
  the steps when they change them. Measured through the real API with `qwen3:8b` (6 methods x 2):

  | | Guardrail fixes | Right grind | English leaking | Median time |
  | --- | --- | --- | --- | --- |
  | Before | 1/12 | 5/12 | 2/12 | 17.5 s |
  | With hints | 0/12 | 12/12 | 0/12 | about 16 s |

  A first version without the dose in the hint made the model propose about 170-215 g of coffee
  for a V60 or Chemex, which is why the dose is now both hinted and bounded.
- **Negative:** someone has to run `ollama serve`. Without it, the buttons answer 503.
- **Negative:** production needs a machine with about 8 GB of free RAM or a GPU. That is not
  decided yet; outside `local` the flag stays off.

## Alternatives Considered
- **Spring AI 2 `ChatClient`:** a provider abstraction we already have through the port, and it
  pulls in Jackson 3. Revisit after the Jackson 3 migration.
- **`llama3.2:3b` (smaller and faster):** garbage in fields and runaway answers in the spike.
- **Free hosted APIs (Groq, OpenRouter, and others):** rate limits and changing terms; fine to
  try, not to depend on.
- **A new `/api/v1/ai/...` module:** it would duplicate the existing endpoints and port.
