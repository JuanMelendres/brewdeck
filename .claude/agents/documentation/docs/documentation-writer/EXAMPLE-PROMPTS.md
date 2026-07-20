# Generic documentation prompts

## Update affected documentation

```text
@documentation-writer Review the current working-tree changes and update all
affected canonical documentation.

Inspect implementation, tests, migrations, configuration, and existing docs.
Do not modify application code, migrations, dependencies, or infrastructure.
Report source mismatches.
```

## Create a TDD

```text
@documentation-writer Create a Technical Design Document for [FEATURE].

Use the approved solution-architect plan and verify assumptions against the
repository. Cover architecture, data, APIs, security, observability,
performance, migrations, testing, rollout, rollback, alternatives, risks, and
open questions.
```

## Create an ADR

```text
@documentation-writer Create an ADR for [DECISION].

Document context, decision drivers, alternatives, rationale, consequences,
risks, and follow-up actions. Do not mark it accepted without approval evidence.
```

## Audit repository documentation

```text
@documentation-writer Audit the repository documentation.

Identify stale, duplicated, contradictory, missing, and orphaned documents.
Create a documentation map and recommend the canonical structure.
Do not delete uncertain documents.
```

# BrewDeck documentation prompts

## BrewSession FDD and TDD

```text
@documentation-writer Create or update the BrewSession FDD and TDD.

Verify Coffee, BrewMethod, Recipe, and BrewSession behavior against the
implementation and approved architecture. Document planned versus actual
parameters, snapshot semantics, ownership, validation, errors, APIs,
persistence, testing, rollout, and the AI recipe suggestions feature flag.
```

## Organize BrewDeck documentation

```text
@documentation-writer Organize BrewDeck documentation under product,
architecture, decisions, api, testing, security, and development (the existing
docs-as-code layout). Preserve useful content, create navigation indexes, and
identify duplicates. Do not relocate docs/superpowers or .claude.
```

## AI recipe suggestions design

```text
@documentation-writer Create a proposed FDD, TDD, and threat-model summary for
the BrewDeck AI recipe suggestions feature (flag: ai_recipe_assistant). Include
tool permissions, confirmation before writes, data isolation, prompt injection,
logging, retention, limits, and evaluation. Clearly label all future behavior as
proposed.
```

# BrickDeck documentation prompts

## Rebrickable integration

```text
@documentation-writer Create or update BrickDeck Rebrickable integration docs.
Document authentication, external IDs, flow, caching, import status,
idempotency, pagination, limits, partial failures, provenance, testing, and
troubleshooting. Verify every detail against the repository.
```

## Theme import TDD

```text
@documentation-writer Create a TDD for complete Rebrickable theme imports.
Use the approved architecture and current set-import implementation. Cover
components, flows, persistence, retries, limits, transactions, resumability,
observability, testing, deployment, and recovery.
```

## BrickDeck domain model

```text
@documentation-writer Document themes, sets, parts, inventories, colors, and
user collections. Distinguish external Rebrickable data from user-owned data.
Include identifier rules, ownership, provenance, and deletion behavior.
```

# Release, API, and operational prompts

## Release notes

```text
@documentation-writer Create release notes for the current release candidate.
Document user-visible changes, API changes, migrations, configuration, security
fixes, breaking changes, upgrade steps, known issues, and rollback limitations.
Do not expose sensitive exploitation details.
```

## API documentation review

```text
@documentation-writer Review API documentation against controllers, DTOs,
validation, error handling, and tests. Correct paths, methods, fields, statuses,
errors, pagination, idempotency, authentication, and authorization.
```

## Deployment runbook

```text
@documentation-writer Create a deployment and recovery runbook for [SERVICE].
Include prerequisites, permissions, safety checks, commands, verification,
failure handling, application rollback, database limitations, escalation, and
post-deployment checks. Use only repository-verified commands.
```

## Pre-merge documentation review

```text
@documentation-writer Review all documentation changes before merge. Validate
links, terminology, source accuracy, Mermaid, commands, current-versus-future
labels, navigation, and that no source files changed.
```
