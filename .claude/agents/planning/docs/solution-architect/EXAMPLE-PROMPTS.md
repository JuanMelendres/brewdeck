# Example prompts for `solution-architect`

## New feature

```text
@solution-architect Analyze the following feature before implementation:

[FEATURE DESCRIPTION]

Inspect the repository, identify existing patterns, compare viable designs, recommend the smallest appropriate architecture, and provide an implementation-ready plan. Include data, API, security, testing, observability, rollout, documentation, and handoff tasks. Do not modify files.
```

## Bug or architectural inconsistency

```text
@solution-architect Investigate the current architecture around [AREA OR BUG]. Trace the flow end to end, identify the architectural root cause, compare safe remediation options, and produce a phased plan that preserves backward compatibility. Do not modify files.
```

## Spring Boot upgrade

```text
@solution-architect Assess the architectural impact of upgrading this project from its current Spring Boot version to [TARGET VERSION]. Inspect Java, Gradle or Maven, security, persistence, tests, Docker, CI/CD, and external integrations. Identify breaking changes, propose a staged migration, and define rollback and validation gates. Do not change dependency versions.
```

## External API integration

```text
@solution-architect Analyze the integration of [PROVIDER/API] into this project. Cover authentication, secret handling, rate limits, pagination, retries, idempotency, caching, provenance, partial failures, data mapping, observability, testing, and vendor-change isolation. Do not modify files.
```

## Database feature

```text
@solution-architect Analyze the data model and migration strategy for [FEATURE]. Inspect existing entities and Flyway conventions. Define constraints, indexes, transaction boundaries, compatibility, backfill, expand-and-contract needs, and rollback limitations. Do not create migrations.
```

## Pull request design review

```text
@solution-architect Review the current branch changes as an architecture gate. Compare the implementation against existing project conventions and the intended requirement. Identify boundary violations, unnecessary complexity, compatibility risks, missing quality gates, and documentation decisions. Do not edit the branch.
```
