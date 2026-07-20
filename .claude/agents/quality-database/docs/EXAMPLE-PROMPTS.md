# Example prompts

These prompts are designed for direct use with the
`database-migration-reviewer` agent.

## Generic current-diff review

```text
@database-migration-reviewer Review all database-related changes in the current
working tree.

Inspect Flyway migrations, JPA entities, repositories, native queries,
configuration, tests, and CI behavior.

Evaluate:
- existing-data compatibility
- ORM-to-schema type alignment
- nullability, defaults, constraints, and indexes
- old/new application and schema compatibility
- locking and table rewrite risk
- backfill safety and resumability
- deployment ordering
- application rollback and roll-forward recovery

Do not modify files.
Do not connect to production, shared staging, or an unknown database.
Report exact evidence and finish with the required review status.
```

## Review an approved architecture plan

```text
@database-migration-reviewer Review the database implementation against the
approved solution-architect plan for [FEATURE].

Confirm that the migration sequence, JPA mappings, constraints, indexes,
backfill, rollout, and recovery strategy match the approved design.

Identify any deviations and classify them by severity.
Do not modify files or migration history.
```

## Detect an edited historical Flyway migration

```text
@database-migration-reviewer Investigate the Flyway checksum failure.

Determine:
- which migration changed
- whether it may already be released
- the Git history of that migration
- whether the schema may differ across environments
- the safest forward-only remediation
- whether a repair action would conceal real drift

Do not execute Flyway repair.
Do not modify the historical migration.
Do not modify any file.
```

## UUID versus BIGINT review

```text
@database-migration-reviewer Review identifier consistency across the feature.

Compare entity identifiers, join columns, DTO assumptions, repository queries,
and Flyway column types.

Pay special attention to UUID versus BIGINT or VARCHAR mismatches, generation
strategy, foreign-key compatibility, and existing data.

Do not modify files. Produce exact remediation tasks for
@spring-backend-engineer and @test-quality-engineer.
```

# BrewDeck prompts

## BrewSession migration review

```text
@database-migration-reviewer Review the BrewSession persistence implementation.

Inspect Coffee, Grinder, BrewMethod, Recipe, and BrewSession relationships.

Evaluate:
- UUID consistency
- recipe snapshot versus live-reference semantics
- dose, water, yield, temperature, time, and sensory numeric precision
- nullability and defaults
- ownership and foreign-key isolation
- timestamp and time-zone semantics
- indexes for recent sessions and history filters
- deletion behavior for coffee, method, recipe, and grinder references
- migration compatibility with existing BrewDeck data
- rollout and forward-fix strategy

Do not modify files.
Do not assume an empty database is representative.
```

## Add a required recipe attribute

```text
@database-migration-reviewer Review the migration that adds a required
[ATTRIBUTE] to existing BrewDeck recipes.

Determine whether legacy rows can be backfilled correctly, whether the default
has valid domain meaning, and whether adding NOT NULL in the same migration is
safe.

Provide a phased expand-and-contract plan when needed.
Do not modify files.
```

## BrewDeck migration history audit

```text
@database-migration-reviewer Audit BrewDeck's full Flyway migration history and
current JPA model for schema drift.

Focus on:
- UUID primary and foreign keys
- renamed or removed fields
- missing constraints
- numeric precision
- timestamp types
- indexes supporting current repository queries
- historical migrations that may have been edited
- test coverage for migrating from previous versions

Do not modify files.
Separate confirmed defects from recommendations.
```

# BrickDeck prompts

## Rebrickable theme import schema

```text
@database-migration-reviewer Review the database changes for importing complete
Rebrickable themes.

Evaluate:
- internal UUIDs versus Rebrickable identifiers
- uniqueness of external theme and set identifiers
- set-number suffix semantics
- idempotent repeated imports
- partial upstream data
- import status and provenance
- duplicate and orphan prevention
- indexes for external lookups and theme queries
- large backfill and transaction risk
- compatibility during deployment

Do not modify files.
```

## Inventory and parts migration

```text
@database-migration-reviewer Review the BrickDeck inventory and parts schema
migration.

Inspect cardinality and uniqueness for:
- sets
- inventories
- parts
- colors
- inventory-part relations
- user collection ownership

Evaluate data volume, composite keys, indexes, duplicate imports, foreign-key
delete behavior, batching, and resumable backfills.

Do not modify files.
Return SPIKE REQUIRED when table size or locking measurements are needed.
```

## Change external-set uniqueness

```text
@database-migration-reviewer Review the proposed uniqueness constraint for
externalSetNumber.

Confirm:
- whether values include Rebrickable version suffixes
- whether uniqueness is global or source-specific
- how null values behave
- whether existing duplicates exist
- how concurrent imports are handled
- how the application maps unique-constraint violations
- whether an online creation strategy is needed

Do not modify files.
```

# Validation-focused prompts

## Migration from previous schema

```text
@database-migration-reviewer Determine whether the test suite validates
migration from the currently deployed schema version rather than only from an
empty database.

Inspect Testcontainers, Flyway test configuration, fixtures, and CI commands.

Report the exact missing scenarios and hand them to
@test-quality-engineer.
Do not modify tests.
```

## PostgreSQL lock-risk review

```text
@database-migration-reviewer Assess the PostgreSQL locking and table-rewrite
risk of the proposed migration.

Do not guess. Identify the PostgreSQL version, affected operations, expected
table-size evidence, and deployment strategy.

When repository evidence is insufficient, define a focused measurement spike
including representative data, commands, metrics, thresholds, and rollback
criteria.
```

## Pre-release review

```text
@database-migration-reviewer Perform the final database review for the current
release candidate.

Review only the database and persistence scope.
List exact blockers, conditional approvals, validation evidence, deployment
checkpoints, recovery steps, and residual risk.

Do not commit, push, merge, tag, or deploy.
```