# Generic incident-response prompts

## Investigate an active incident

```text
@incident-response-engineer Investigate the reported incident.

Build a timeline, identify impact, preserve evidence, separate facts from
hypotheses, assess data and security risk, and propose the safest mitigation and
recovery verification.

Do not access production or execute operational changes.
```

## Analyze a failed deployment

```text
@incident-response-engineer Analyze the failed deployment using repository
history, CI evidence, container configuration, migration behavior, and sanitized
logs.

Determine whether rollback is safe, whether roll-forward is required, and what
verification must occur.

Do not deploy, restart, or roll back systems.
```

## Create a postmortem

```text
@incident-response-engineer Create a blameless postmortem from the available
incident evidence.

Include impact, timeline, root cause, trigger, contributing factors, detection
gaps, response gaps, recovery, corrective actions, owners or agent handoffs, and
completion evidence.

Do not invent times or facts.
```

## Repeated incident review

```text
@incident-response-engineer Compare the latest incident with previous similar
failures.

Identify recurring mechanisms, incomplete corrective actions, alerting gaps,
runbook gaps, and systemic prevention opportunities.
```

# BrewDeck incident prompts

## BrewSession failures

```text
@incident-response-engineer Investigate repeated BrewSession creation failures.

Trace validation, authorization, relationships, transactions, Flyway schema,
PostgreSQL constraints, connection pools, API errors, and frontend behavior.

Assess whether any sessions are missing, duplicated, or partially saved.
```

## Ownership exposure

```text
@incident-response-engineer Investigate a suspected BrewDeck ownership or data
isolation incident.

Determine whether one user could access another user's coffees, recipes,
or sessions.

Preserve evidence, avoid exposing personal data, and coordinate immediately
with @security-auditor.
```

## AI recipe suggestions degradation

```text
@incident-response-engineer Investigate AI recipe suggestions timeouts and elevated errors.

Review feature flags, request deadlines, rate limits, retries, external service
health, user-data isolation, cost safeguards, fallback behavior, and frontend
messaging.

Do not call production AI services.
```

# BrickDeck incident prompts

## Rebrickable outage or 401

```text
@incident-response-engineer Investigate BrickDeck Rebrickable import failures.

Differentiate credential failure, upstream outage, rate limiting, timeout,
malformed response, and local mapping failure.

Assess cache behavior, partial imports, user impact, and safe recovery.
Do not read or expose the API key.
```

## Duplicate imports

```text
@incident-response-engineer Investigate duplicate BrickDeck set or theme imports.

Review idempotency, uniqueness, concurrent requests, retries, transaction
boundaries, job execution, and existing data.

Propose reconciliation without deleting user collection data.
```

## Theme import stall

```text
@incident-response-engineer Investigate a stalled complete-theme import.

Review pagination checkpoints, rate limits, retries, transaction duration,
memory, database connections, background worker state, and observability.

Determine whether processing can resume safely.
```

# Operational and postmortem prompts

## Database incident

```text
@incident-response-engineer Investigate the database incident using sanitized
logs and repository evidence.

Review connection saturation, lock waits, long transactions, migrations,
constraints, backfills, query behavior, and application compatibility.

Do not run destructive SQL or Flyway repair.
```

## CI outage

```text
@incident-response-engineer Investigate repeated CI failures across pull
requests.

Review runner or dependency outage, action versions, permissions, caches,
runtime versions, flaky tests, external scanners, artifacts, and concurrency.

Separate repository defects from provider incidents.
```

## Alert quality review

```text
@incident-response-engineer Review the alerts associated with the incident.

Identify missed detection, delayed detection, noisy alerts, missing context,
incorrect thresholds, and absent runbook links.

Create platform handoffs for measurable improvements.
```

## Remediation verification

```text
@incident-response-engineer Verify whether corrective actions IR-001 through
IR-008 would prevent or detect recurrence.

Review implementation, tests, dashboards, alerts, runbooks, deployment safety,
and residual risk.

Do not modify application code.
```
