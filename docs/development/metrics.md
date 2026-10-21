# Metrics

The API publishes Micrometer metrics in Prometheus format on the management port:
`http://localhost:8081/actuator/prometheus` ([ADR-023](../decisions/ADR-023-application-metrics.md)).
Cloud Run never routes that port, so on staging nothing outside the container can read them, and
nothing collects them yet (export is ADR-023 PR 3). To look at them locally, start Prometheus and
Grafana as described in [setup.md](setup.md).

Every metric carries two common tags: `application` (`brewdeck-api`) and `environment` (`local`,
`test`, `staging` or `prod`).

## BrewDeck metrics

Names below are Micrometer names. Prometheus turns the dots into underscores and adds a suffix:
`_total` for counters; `_seconds_count`, `_seconds_sum` and `_seconds_bucket` for timers.

| Metric | Type | Tags | What it counts |
|---|---|---|---|
| `brewdeck.auth.logins` | counter | `outcome`: `success`, `failure` | Login attempts that reached the password check. Wrong password and unknown email are both `failure`. Rate-limited logins are not here; they are under `brewdeck.rate.limit.rejections`. |
| `brewdeck.auth.refresh.reuse` | counter | none | A used or revoked refresh token came back. Every session of that user was revoked. |
| `brewdeck.rate.limit.rejections` | counter | `rule`: a `RateLimitRule` in lower case, such as `login_ip` | Requests rejected with 429. |
| `brewdeck.password.hash` | timer | `operation`: `encode`, `matches` | Time spent in Argon2id, after getting a permit (ADR-018). |
| `brewdeck.password.hash.wait` | timer | none | Time spent waiting for one of the 4 hashing permits. If it grows, logins are queueing. |
| `brewdeck.mail.sent` | counter | `kind`: the email template, such as `verification`; `outcome`: `success`, `failure` | SMTP sends. Only when `BREWDECK_MAIL_ENABLED=true`; the logging adapter records nothing. |
| `brewdeck.ai.requests` | timer | `operation`: `suggest`, `improve`; `provider`: `claude`, `ollama`; `outcome`: `success`, `unavailable`, `error` | Calls to the AI provider and how long they took. `unavailable` is the 503 case. |
| `brewdeck.retention.accounts` | counter | `result`: `reminded`, `deleted`, `failed` | Accounts handled by the retention purge (ADR-021). |
| `brewdeck.retention.tokens.deleted` | counter | none | Expired token rows the retention purge deleted. |

The timers publish histogram buckets, so percentiles can be computed across instances, for
example:

```promql
histogram_quantile(0.95, sum by (le, operation) (rate(brewdeck_ai_requests_seconds_bucket[5m])))
```

The retention purge runs as a Cloud Run Job that exits right after the run, so nothing scrapes its
metrics. They only become useful once metrics are pushed (ADR-023 PR 3). Until then, the job's
log line `Retention purge done: ...` is the record.

## Framework metrics

Spring Boot and Micrometer also publish, among others:

- `http.server.requests`: every request, tagged with `uri`, `method` and `status`, with histogram
  buckets.
- `jvm.*`, `process.*`, `system.*`: memory, garbage collection, threads, CPU.
- `hikaricp.*`: the database connection pool.
- `cache.*` for the `featureFlags` cache: `cache.gets` (tag `result`: `hit`, `miss`),
  `cache.evictions`, `cache.size`.

## Rule for new metrics

Tag values must come from a small, fixed set. Never use a user id, email, IP address, token or
free text as a tag. Each distinct value creates a new time series, and those values are personal
data. Add new BrewDeck metrics to `common/metrics/BrewdeckMetrics.java` and to the table above.
