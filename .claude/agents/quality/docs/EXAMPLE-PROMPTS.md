# Example prompts for `test-quality-engineer`

These prompts assume the agent is installed under `.claude/agents/quality/`.

## Generic post-implementation review

```text
@test-quality-engineer Review the current implementation against the approved solution-architect plan and acceptance criteria.

Inspect the diff and existing test conventions. Build a risk-based test plan, identify missing high-value scenarios, and add the necessary tests without changing production behavior.

Run the narrowest relevant tests first, then the full validation required by the repository. Report the exact commands, results, defects, assumptions, and remaining risks.

Do not commit, push, merge, or release.
```

## Review only, no edits

```text
@test-quality-engineer Perform a read-only quality review of the current branch.

Do not modify files. Identify weak or missing unit, controller, repository, integration, and external-service tests. Rank findings by regression and business risk, and provide an implementation-ready test plan.
```

## Confirmed defect regression test

```text
@test-quality-engineer Reproduce the reported defect and add a failing regression test that demonstrates the expected behavior.

Do not fix production code. Run the focused test, capture the failure evidence, and hand the implementation fix to spring-backend-engineer.
```

# BrewDeck prompts

## BrewMethod API review

```text
@test-quality-engineer Review the BrewMethod backend implementation.

Verify successful creation and retrieval, validation errors, duplicate names when applicable, missing resources, UUID handling, HTTP status codes, error response shape, PostgreSQL persistence, and Flyway startup from a clean database.

Follow existing BrewDeck testing conventions. Add only high-value tests and do not change production behavior.
```

## BrewSession behavior

```text
@test-quality-engineer Review the BrewSession implementation against the approved architecture plan.

Cover:
- Successful session creation
- Missing Coffee, Recipe, BrewMethod, or Grinder references
- Invalid or negative coffee dose and water values
- Temperature and duration boundaries defined by the domain
- Planned values versus actual session values
- Sensory evaluation validation
- Transaction rollback after persistence failure
- History ordering and filtering
- Duplicate or repeated submission behavior
- UUID consistency across API, JPA, Flyway, and PostgreSQL

Use unit, MockMvc, repository, and Testcontainers tests only where each level adds value. Do not modify production behavior. Run relevant Gradle or Maven validation and report evidence.
```

## BrewDeck migration verification

```text
@test-quality-engineer Validate the tests around the newest BrewDeck Flyway migration.

Start PostgreSQL from a clean Testcontainers database, confirm all migrations apply, and verify the new entity mappings and constraints through behavior-focused integration tests.

Do not edit a released migration. Report any database concern to database-migration-reviewer.
```

# BrickDeck prompts

## Rebrickable set import

```text
@test-quality-engineer Review the Rebrickable set import implementation.

Add or improve deterministic tests for:
- Successful external import
- Local cache hit
- 401 Unauthorized
- 404 Not Found
- 429 rate limit
- Timeout and connection failure
- Malformed JSON
- Partial upstream data
- Duplicate and idempotent imports
- Transaction rollback on persistence failure
- Correct IMPORTED_FROM_REBRICKABLE and LOCAL_CACHE_HIT transitions
- API key absence from logs and responses

Do not call the real Rebrickable API. Do not change production behavior. Execute the relevant test and build commands and report exact outcomes.
```

## Theme import pagination

```text
@test-quality-engineer Review the complete LEGO theme import feature.

Focus on pagination termination, repeated pages, duplicate sets, partial page failures, retries, idempotency, stale cache behavior, and transaction boundaries.

Use deterministic fixtures or a local HTTP stub. Identify which scenarios need unit tests versus Spring integration tests. Add the tests and run validation without committing or pushing.
```

## Existing cache regression

```text
@test-quality-engineer Verify that a previously imported LEGO set returns LOCAL_CACHE_HIT without making an unnecessary external request.

Add a regression test that asserts both the response and the absence of an external client call. Do not change production code unless I explicitly authorize the fix after reviewing the evidence.
```

# Pull request review

```text
@test-quality-engineer Review the current branch as an independent pull-request quality gate.

Compare it with the target branch using safe Git diff commands. Do not edit production files. Add missing tests only when they are clearly within the requested feature scope.

Return one status: PASSED, PASSED WITH WARNINGS, CHANGES REQUIRED, or BLOCKED. Include exact commands and concise evidence for every blocking finding.
```

# Flaky test investigation

```text
@test-quality-engineer Investigate the flaky test [TEST NAME].

Reproduce it with the narrowest command. Inspect shared mutable state, time, randomness, asynchronous execution, database cleanup, ports, thread scheduling, and external stubs.

Do not hide the issue with retries, arbitrary sleeps, or weakened assertions. Propose and, when safe, implement a deterministic test-only correction. Report whether the defect is in the test, production code, or environment.
```