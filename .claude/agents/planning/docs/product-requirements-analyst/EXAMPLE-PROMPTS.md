# Generic product requirements prompts

## Turn an idea into requirements

```text
@product-requirements-analyst Convert this idea into implementation-ready product
requirements: [IDEA].

Define the problem, actors, current and desired behavior, goals, non-goals,
journeys, business rules, permissions, validation, errors, acceptance criteria,
MVP, feature flags, analytics, risks, dependencies, and open questions.

Do not choose the technical architecture or modify source code.
```

## Review an existing feature request

```text
@product-requirements-analyst Review the current feature request for ambiguity,
missing business rules, scope creep, permissions, edge cases, acceptance
criteria, MVP boundaries, and contradictions.

Produce a corrected requirements package.
```

## Convert a bug into requirements

```text
@product-requirements-analyst Analyze this bug as a product behavior gap:
[BUG DESCRIPTION].

Define current behavior, expected behavior, affected actors, impact, business
rule, regression acceptance criteria, data implications, and non-goals.

Do not prescribe a code fix.
```

## Backlog decomposition

```text
@product-requirements-analyst Decompose the approved feature into coherent
backlog items.

For each item include goal, scope, dependencies, acceptance criteria, feature
flag, agent owner, and definition of done.

Avoid splitting work into technically convenient but unusable partial flows.
```

# BrewDeck product requirements prompts

## BrewSession requirements

```text
@product-requirements-analyst Define the complete BrewSession product
requirements.

Clarify:
- recipe versus session
- planned versus actual values
- grind setting
- dose, water, temperature, time, yield, and units
- sensory rating
- tasting notes
- history
- editing and deletion
- ownership
- validation
- duplicate submission
- offline or interrupted flow
- feature flags
- MVP

Produce FDD-ready requirements.
```

## Recipe sharing

```text
@product-requirements-analyst Define product requirements for BrewDeck recipe
sharing.

Cover private, shared, and public visibility; ownership; copying versus editing;
attribution; revocation; deleted users; direct links; search; privacy; abuse;
feature flags; MVP; and acceptance criteria.

Do not choose the storage or API architecture.
```

## AI recipe suggestions

```text
@product-requirements-analyst Define product requirements for the BrewDeck AI
Barista.

Cover:
- user problem
- eligible data
- recommendation explanation
- uncertainty
- confirmation before writes
- user feedback
- privacy
- rate limits
- failure behavior
- feature flags
- analytics
- guardrails
- MVP and later phases

Treat AI output as a suggestion, not a guaranteed result.
```

# BrickDeck product requirements prompts

## Set import

```text
@product-requirements-analyst Define the BrickDeck set-import requirements.

Cover set-number validation, external source, import status, local cache hit,
fresh import, partial data, duplicate import, retry, rate limit, timeout, user
feedback, provenance, feature flags, and acceptance criteria.
```

## User collection

```text
@product-requirements-analyst Define product requirements for BrickDeck user
collections.

Cover ownership, quantities, duplicates, private/public visibility, notes,
deletion, external refresh behavior, search, filters, import, export, MVP, and
acceptance criteria.

Protect user-owned data from external refreshes.
```

## Complete theme import

```text
@product-requirements-analyst Define requirements for complete Rebrickable theme
imports.

Cover who can start an import, progress, partial completion, retries,
cancellation, duplicate imports, rate limits, resume behavior, provenance,
background processing, notifications, and MVP.
```

## Future marketplace

```text
@product-requirements-analyst Define product requirements for future BrickDeck
marketplace and price comparison.

Cover seller/source attribution, freshness, regional availability, currencies,
redirects, user trust, affiliate disclosure when applicable, privacy, failure
behavior, MVP, and non-goals.
```

# Planning, feature flags, and validation prompts

## MVP review

```text
@product-requirements-analyst Review the proposed MVP for [FEATURE].

Identify missing core value, incomplete workflows, unnecessary scope, required
permissions, required error states, feature-flag needs, and deferred work.

Return a coherent MUST HAVE, SHOULD HAVE, COULD HAVE, and NOT NOW breakdown.
```

## Feature-flag plan

```text
@product-requirements-analyst Define the product behavior for feature flags in
[FEATURE].

For each flag specify purpose, default, eligible users, environments,
dependencies, disabled behavior, success criteria, and removal criteria.

Do not use feature flags to hide authorization or data-integrity gaps.
```

## Acceptance-criteria audit

```text
@product-requirements-analyst Audit all acceptance criteria for [FEATURE].

Ensure they are observable, testable, non-duplicative, and cover success,
validation, permissions, errors, conflicts, partial failures, refresh, feature
flags, accessibility, and recovery.
```

## Requirement traceability

```text
@product-requirements-analyst Create a traceability matrix connecting product
goals, functional requirements, business rules, acceptance criteria, backlog
items, and specialist agent handoffs.

Highlight any requirement with no acceptance criteria or no implementation
owner.
```
