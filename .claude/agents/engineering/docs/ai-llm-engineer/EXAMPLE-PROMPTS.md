# Generic AI and LLM prompts

## Design an AI feature

```text
@ai-llm-engineer Design the approved AI capability for [FEATURE].

Define the AI justification, responsibility boundary, data policy, model,
prompt, context, structured output, tools, confirmation, injection defenses,
cost, latency, fallback, evaluations, observability, feature flag, and rollout.

Do not implement until the evaluation and safety boundaries are explicit.
```

## Implement a structured-output feature

```text
@ai-llm-engineer Implement the approved structured-output AI feature.

Use a typed schema, validate all fields, reject unknown or unsafe values, add a
bounded repair path, and fall back safely when validation fails.

Add mock-provider tests and an offline evaluation dataset.
```

## AI security review

```text
@ai-llm-engineer Review the AI feature for prompt injection, data leakage,
cross-user access, excessive tool permissions, unsafe writes, missing
confirmation, unbounded context, secret exposure, and logging risk.

Create exact remediation handoffs.
```

## AI evaluation suite

```text
@ai-llm-engineer Create a versioned evaluation suite for [FEATURE].

Include representative, edge, adversarial, refusal, privacy, no-evidence,
tool-use, and regression cases.

Define measurable pass criteria before changing prompts or models.
```

# BrewDeck AI prompts

## AI recipe suggestions

```text
@ai-llm-engineer Implement the approved BrewDeck AI recipe suggestions foundation.

Use only authorized coffee, recipe, and BrewSession context.
Return structured recommendations with rationale, uncertainty, and source
session references.

Do not write changes without user confirmation.
Keep manual brewing workflows available when AI fails.
```

## Brew troubleshooting

```text
@ai-llm-engineer Design and implement brew troubleshooting suggestions.

Inputs may include recipe, actual session values, tasting notes, and sensory
ratings.

Keep unit conversion and domain validation deterministic.
The model may suggest adjustments but must not present them as guaranteed.
```

## BrewDeck RAG

```text
@ai-llm-engineer Design a RAG system for BrewDeck coffee and brewing knowledge.

Define authorized sources, chunking, metadata, freshness, retrieval filters,
citation correctness, prompt injection defense, insufficient-evidence behavior,
evaluation, cost, and reindex strategy.
```

## AI write confirmation

```text
@ai-llm-engineer Implement the confirmation boundary for applying an AI
recommendation to a BrewDeck recipe.

The model may propose typed changes.
Backend authorization, validation, user confirmation, and persistence must
remain deterministic.
```

# BrickDeck AI prompts

## Set recommendations

```text
@ai-llm-engineer Design BrickDeck set recommendations using the user's approved
collection data and external set metadata.

Do not invent sets or identifiers.
Show recommendation reasons, known data, assumptions, provenance, and fallback
when evidence is insufficient.
```

## Spare-parts suggestions

```text
@ai-llm-engineer Design build suggestions from spare parts.

Separate exact inventory matches from estimated substitutions.
Return structured part requirements, confidence, missing pieces, and source
provenance.

Do not modify the user's collection without confirmation.
```

## Piece classification

```text
@ai-llm-engineer Design an image-assisted LEGO piece-classification workflow.

Define supported image quality, candidate results, confidence, user correction,
privacy, retention, evaluation dataset, false-positive behavior, and fallback to
manual search.
```

## Natural-language collection search

```text
@ai-llm-engineer Implement natural-language collection search using a
deterministic query plan generated from a validated schema.

The model must not generate raw SQL.
Backend authorization and query limits remain deterministic.
```

# RAG, tools, evaluations, and operations prompts

## Tool registry

```text
@ai-llm-engineer Define the approved AI tool registry.

For every tool document authentication, authorization, input schema, side
effects, idempotency, confirmation, timeouts, retries, rate limits, audit
events, and failure behavior.

Reject tools that are excessively broad.
```

## Prompt version migration

```text
@ai-llm-engineer Upgrade prompt version [OLD] to [NEW].

Document the behavioral change, evaluation dataset, baseline, regression
results, rollout flag, guardrails, and rollback target.

Do not deploy the new prompt automatically.
```

## Provider fallback

```text
@ai-llm-engineer Design provider and model fallback behavior.

Define which failures permit fallback, compatibility of structured output,
latency and cost budgets, privacy constraints, user messaging, metrics, and
circuit-breaking behavior.
```

## AI incident readiness

```text
@ai-llm-engineer Create AI incident readiness for [FEATURE].

Cover provider outage, elevated latency, cost spike, prompt regression, invalid
structured output, unsafe tool attempts, data leakage suspicion, feature-flag
disablement, fallback, evidence preservation, and postmortem inputs.
```
