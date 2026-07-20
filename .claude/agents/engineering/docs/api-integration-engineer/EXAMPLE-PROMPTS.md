# Generic integration prompts

## Implement a third-party API integration

```text
@api-integration-engineer Implement the approved integration with [SERVICE].

Verify the upstream contract and repository conventions.

Cover authentication, transport DTOs, mapping, validation, explicit timeouts,
bounded retries, rate limits, pagination, idempotency, stable error translation,
metrics, tests, and documentation.

Do not expose secrets or change public API contracts silently.
```

## Review an existing integration

```text
@api-integration-engineer Audit the existing [SERVICE] integration.

Review contract drift, authentication, timeouts, retries, rate limits,
pagination, data validation, idempotency, cache behavior, observability,
security, and failure-mode tests.

Implement focused integration fixes only.
```

## Webhook implementation

```text
@api-integration-engineer Implement the approved webhook receiver for [SERVICE].

Cover signature verification, replay protection, payload limits, event IDs,
idempotency, ordering, unknown events, retries, auditability, and safe responses.

Do not process a webhook before authenticating the sender.
```

## Resilience review

```text
@api-integration-engineer Review the resilience policy for all outbound HTTP
clients.

Identify missing or unsafe timeouts, retries, backoff, jitter, rate-limit
handling, circuit breakers, fallbacks, and metrics.

Do not add generic retries to non-idempotent operations.
```

# BrewDeck integration prompts

## External coffee catalog

```text
@api-integration-engineer Design and implement an approved external coffee
catalog integration for BrewDeck.

Preserve source, import timestamp, freshness, and the distinction between
external and user-entered coffee data.

Cover API credentials, pagination, rate limits, partial data, duplicate coffees,
remote images, mapping, cache behavior, and failure tests.

Do not overwrite user-owned data during refresh.
```

## AI recipe suggestions service client

```text
@api-integration-engineer Implement the server-side client boundary for the
approved BrewDeck AI recipe suggestions service.

Cover user-context isolation, request minimization, timeouts, rate limits,
structured errors, retries only when safe, logging redaction, feature flags,
metrics, and explicit confirmation before write operations.

Do not expose model credentials to the frontend.
```

# BrickDeck integration prompts

## Rebrickable client review

```text
@api-integration-engineer Review and improve the BrickDeck Rebrickable client.

Cover:
- API-key handling
- explicit timeouts
- 401 and 403
- 404
- 429 and Retry-After
- transient 5xx
- pagination
- malformed JSON
- missing fields
- duplicate records
- partial data
- cache status
- provenance
- idempotent imports
- metrics
- contract tests

Do not expose the API key.
```

## Complete theme import

```text
@api-integration-engineer Implement the approved Rebrickable complete-theme
import integration.

Use bounded pagination, checkpointing, rate-limit handling, idempotency,
provenance, partial-failure states, duplicate protection, and resumable
processing.

Coordinate persistence changes with @database-migration-reviewer.
```

## Marketplace integration spike

```text
@api-integration-engineer Perform a technical spike for a future BrickDeck
marketplace and price-comparison integration.

Compare available API contracts, authentication, limits, freshness, product
matching, redirects, remote URLs, provenance, legal usage constraints,
operational cost, and failure modes.

Do not implement production scraping without an approved architecture.
```

# Testing and operations prompts

## Contract-test suite

```text
@api-integration-engineer Build or improve the HTTP contract-test suite for
[SERVICE].

Cover success, authentication failure, forbidden, not found, rate limit,
Retry-After, timeout, selected 5xx, malformed JSON, missing fields, wrong
content type, pagination, duplicates, and partial data.

Use a local mock server.
Do not call production.
```

## Integration release validation

```text
@api-integration-engineer Perform release validation for the [SERVICE]
integration.

Verify the effective configuration model, build, unit tests, contract tests,
integration tests, error mapping, retry behavior, metrics, and documentation.

Do not use real production credentials or deploy.
```

## Incident analysis

```text
@api-integration-engineer Analyze the recent [SERVICE] integration failure.

Use logs and repository evidence without exposing secrets.

Determine the failure category, retry behavior, idempotency impact, partial data,
recovery path, missing observability, and required remediation.

Do not replay unsafe production requests.
```

## Rate-limit strategy

```text
@api-integration-engineer Design the rate-limit and backpressure strategy for
[SERVICE].

Account for documented quotas, user-triggered requests, background jobs,
concurrency, Retry-After, queueing, cache, fairness, metrics, and graceful
degradation.
```
