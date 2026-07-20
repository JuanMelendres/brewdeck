# Generic performance and reliability prompts

## Performance audit

```text
@performance-reliability-engineer Audit the current application performance and
reliability.

Identify critical user journeys, likely bottlenecks, missing measurements,
database risks, external dependency risks, frontend performance, concurrency,
timeouts, retries, and capacity assumptions.

Do not optimize without a baseline.
```

## Investigate slow endpoint

```text
@performance-reliability-engineer Investigate [METHOD] [ENDPOINT].

Trace controller, service, repository, serialization, database queries, and
external calls.

Create a repeatable baseline, identify the dominant bottleneck, implement one
approved focused change, and compare before and after.

Do not weaken correctness or security.
```

## Load-test plan

```text
@performance-reliability-engineer Create and implement a local load-test plan for
[USER JOURNEY].

Define workload, dataset, warm-up, duration, concurrency, targets, metrics,
failure criteria, and reporting.

Do not target production or third-party services.
```

## Reliability review

```text
@performance-reliability-engineer Review reliability for [FEATURE].

Evaluate timeouts, retries, idempotency, backpressure, queues, concurrency,
partial failure, graceful degradation, shutdown, observability, and recovery.

Separate architecture blockers from implementation defects.
```

# BrewDeck prompts

## BrewSession creation

```text
@performance-reliability-engineer Measure and review BrewSession creation.

Cover validation, relationship lookups, transactions, inserts, serialization,
concurrent duplicate requests, database connections, and response latency.

Use representative local data and report before-and-after evidence.
```

## Session history

```text
@performance-reliability-engineer Review BrewDeck session-history performance.

Evaluate pagination, filtering, indexes, N+1 queries, sorting, payload size,
frontend rendering, slow-network behavior, and user-data cache isolation.

Coordinate index changes with @database-migration-reviewer.
```

## AI recipe suggestions

```text
@performance-reliability-engineer Define performance and reliability budgets for
the future BrewDeck AI recipe suggestions.

Cover end-to-end latency, model timeout, retries, rate limits, cost limits,
concurrency, caching, user cancellation, graceful degradation, metrics, and
feature-flag rollback.

Do not call paid or production AI services.
```

# BrickDeck prompts

## Rebrickable import

```text
@performance-reliability-engineer Measure and review BrickDeck set import.

Cover upstream latency simulation, retries, pagination, mapping, database
writes, transaction size, duplicate imports, connection-pool use, and partial
failure.

Use a local mock server and disposable PostgreSQL.
```

## Complete theme import

```text
@performance-reliability-engineer Create a load and soak test for complete
Rebrickable theme imports.

Measure throughput, memory, database connections, transaction duration, rate
limit behavior, retries, duplicate prevention, and recovery.

Do not call Rebrickable during the load test.
```

## Set search

```text
@performance-reliability-engineer Review BrickDeck set-search performance.

Evaluate query plans, indexes, pagination, filtering, result payload, remote
images, frontend rendering, and large datasets.

Use representative generated data.
```

# Capacity and operations prompts

## Capacity plan

```text
@performance-reliability-engineer Create a capacity model for [SERVICE].

Document assumptions for users, requests, imports, data growth, concurrency,
CPU, memory, database connections, storage, and external quotas.

Clearly separate measured facts from estimates.
```

## Performance regression gate

```text
@performance-reliability-engineer Design a CI performance regression gate for
[CRITICAL JOURNEY].

Use a stable local benchmark, tolerance thresholds, repeat runs, artifact
reporting, and false-positive controls.

Coordinate CI changes with @devops-platform-engineer.
```

## Memory investigation

```text
@performance-reliability-engineer Investigate suspected memory growth in
[SERVICE OR JOB].

Create a reproducible soak scenario, collect safe local JVM or Node evidence,
distinguish retained memory from normal cache behavior, and propose a focused
fix.

Do not use production heap dumps.
```

## Database saturation

```text
@performance-reliability-engineer Investigate database connection saturation.

Review pool size, request concurrency, transaction duration, slow queries,
timeouts, retries, background jobs, and PostgreSQL capacity.

Do not increase the pool until the bottleneck and database capacity are known.
```
