# Generic pull-request review prompts

## Review current working tree

```text
@pull-request-reviewer Perform a final independent review of the current
working-tree changes.

Compare the implementation with the approved requirement and architecture.
Review correctness, API compatibility, database changes, security, testing,
performance, platform, documentation, and scope.

Do not modify files.
Finish with APPROVE, APPROVE WITH NON-BLOCKING COMMENTS, CHANGES REQUESTED, or
BLOCKED.
```

## Review a feature branch

```text
@pull-request-reviewer Review the complete diff between the current branch and
[BASE_BRANCH].

Identify unrelated changes, hidden breaking changes, missing tests, unsafe
migrations, authorization defects, external-integration risks, operational
gaps, and documentation mismatches.

Do not edit, commit, push, merge, or approve remotely.
```

## Re-review resolved findings

```text
@pull-request-reviewer Re-review findings PR-001 through PR-006.

Verify the remediation diff, relevant tests, compatibility, and specialist
evidence.

Do not repeat resolved findings unless the defect remains.
```

## Focused regression review

```text
@pull-request-reviewer Review the change specifically for regressions.

Trace all affected callers, API consumers, database records, feature flags,
error paths, and deployment overlap.

List only evidence-based findings.
```

# BrewDeck pull-request review prompts

## BrewSession PR

```text
@pull-request-reviewer Review the BrewSession pull request.

Compare it with the approved FDD and TDD.

Verify:
- recipe versus session semantics
- planned and actual values
- numeric units and precision
- user ownership
- UUID and Flyway consistency
- API errors
- frontend states
- session-history queries
- tests
- feature flags
- documentation

Do not modify files.
```

## AI recipe suggestions PR

```text
@pull-request-reviewer Review the feature-flagged AI recipe suggestions pull request.

Focus on user-data isolation, prompt and tool boundaries, confirmation before
writes, error handling, timeouts, rate limits, logging, cost controls,
frontend messaging, tests, rollback, and documentation.

Request specialist security review when evidence is incomplete.
```

## Recipe management PR

```text
@pull-request-reviewer Review the BrewDeck recipe-management change.

Verify ownership, validation, numeric precision, duplicate behavior, API
compatibility, frontend loading and error states, database migration safety,
tests, and documentation.
```

# BrickDeck pull-request review prompts

## Rebrickable import PR

```text
@pull-request-reviewer Review the Rebrickable set-import pull request.

Verify:
- API-key safety
- external identifier semantics
- 401, 404, 429, timeout, and malformed-response handling
- retries
- idempotency
- cache status
- provenance
- partial data
- duplicate imports
- tests
- observability
- documentation

Do not modify files.
```

## Complete theme import PR

```text
@pull-request-reviewer Review the complete-theme-import change.

Focus on pagination, rate limits, checkpointing, transaction size, concurrent
imports, duplicate prevention, partial failure, user-data safety, performance,
deployment, and recovery.
```

## Collection management PR

```text
@pull-request-reviewer Review the BrickDeck user-collection change.

Verify ownership, public versus private visibility, duplicate handling,
quantities, external refresh behavior, authorization tests, frontend states,
database constraints, and API compatibility.
```

# Release and final-gate prompts

## Release candidate review

```text
@pull-request-reviewer Perform a final release-candidate review of all changes
since [LAST_RELEASE_TAG].

Review feature completeness, breaking changes, migrations, security findings,
dependency changes, container and CI behavior, observability, rollback,
known issues, and release documentation.

Do not release or deploy.
```

## Dependency-upgrade PR

```text
@pull-request-reviewer Review the dependency and Spring Boot upgrade PR.

Verify the effective dependency tree, breaking changes, configuration changes,
runtime behavior, tests, Docker images, CVE remediation evidence, rollback, and
documentation.

Do not accept scanner suppression as remediation without evidence.
```

## Migration-heavy PR

```text
@pull-request-reviewer Review the database-heavy pull request.

Verify existing-data compatibility, migration ordering, immutable history,
backfills, constraints, indexes, deployment overlap, application rollback,
forward-fix recovery, and migration tests.

Request @database-migration-reviewer re-review for unresolved database risk.
```

## Final documentation gate

```text
@pull-request-reviewer Review whether the pull request documentation matches the
actual implementation.

Check FDD, TDD, ADRs, API docs, runbooks, release notes, environment variables,
migration notes, security notes, and known limitations.

Do not request documentation that is unrelated to the change.
```
