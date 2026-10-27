# ADR-023: Application metrics with Micrometer and Prometheus

## Status
Accepted (2026-10-21). Backlog OBS-01.

## Date
2026-10-21

## Context
- `spring-boot-starter-actuator` already brings Micrometer. JVM, HTTP, Hikari and Tomcat metrics
  are collected in memory, but only `health` and `info` are exposed, and nothing is exported.
- Today, the only way to see how the API behaves is through logs, for example:
  - latency per endpoint;
  - errors by status;
  - connection pool use;
  - heap use (the staging OOMs before #291).
- Cloud Run exposes one port, the one in `PORT`, and that port is public. Anything served on it is
  reachable from the internet.
- `/actuator/health` on the main port is public today. The deploy pipeline uses it to check the
  candidate revision, and so do the E2E workflow and the Postman collection.

## Decision

**PR 1 (this ADR): Prometheus format on a private port.**
- Add `micrometer-registry-prometheus`. Its version is managed by Spring Boot.
- The actuator moves to its own port: `management.server.port`, env `MANAGEMENT_SERVER_PORT`,
  default 8081. It exposes `health`, `info` and `prometheus` there.
  - Cloud Run never routes that port, so the metrics are not public.
  - A first `SecurityFilterChain` matches requests by the bound management port and permits them.
    That port serves no API.
  - On the main port, `/actuator/**` gets 401.
- **The public health check moves to `/health` on the main port**, as the `public` health group
  (`additional-path`), without details.
  - Spring only allows a one-segment path there, so `/actuator/health` could not be kept on the
    main port.
  - Not `/healthz`: Cloud Run reserves some paths that end in `z`, and its front end answers 404
    without reaching the container. The first staging deploy of this change failed on it
    (traffic stayed on the old revision), so the path is `/health`.
  - Updated: the deploy pipeline, the E2E workflow, `SecurityConfig`, tests, docs and Postman.
- **Tags:**
  - `application` comes from `spring.application.name`.
  - `environment` comes from the feature-flag resolver: local, test, staging or prod.
- **Latency histograms:** `http.server.requests` publishes buckets, so Prometheus can compute
  percentiles across instances.
- **Local:** a `metrics` profile in Docker Compose starts Prometheus and Grafana.
  - Prometheus scrapes `host.docker.internal:8081`.
  - Grafana is anonymous and provisioned with the Prometheus data source.
  - Both bind to localhost only.

**PR 2: BrewDeck metrics** (built; names and tags in
[metrics.md](../development/metrics.md)).
- Counters and timers for:
  - login success and failure;
  - rate-limit rejections;
  - Argon2id hash time and wait (ADR-018);
  - email send failures;
  - AI latency and errors;
  - refresh-token reuse;
  - retention purge results.
- `recordStats()` on the `featureFlags` cache.
- **No user id, email or token as a tag**, for cardinality and privacy.

**PR 3: export from staging over OTLP to Grafana Cloud** (owner decision 2026-10-27).
- Candidates were Grafana Cloud free over OTLP push and Google Cloud Monitoring. Grafana Cloud
  won: it works the same from any host (PROD-02 is still open, and a home lab is an option), it
  has dashboards, and its free tier covers staging. Cloud Monitoring needs no secret, but it
  charges for custom metrics past a small allowance, and it ties the metrics to GCP.
- `micrometer-registry-otlp` pushes every minute. In Boot 4 it is only auto-configured with
  `spring-boot-opentelemetry` on the classpath, which brings the OpenTelemetry API and SDK; no
  traces or logs are exported.
- Off by default (`OTLP_METRICS_ENABLED=false`): local runs and tests send nothing. When it is on,
  startup fails without `OTLP_METRICS_URL` and `OTLP_METRICS_AUTHORIZATION`, so a missing secret
  shows up as a failed deploy, not as silence.
- Histograms are sent as base-2 exponential histograms. Each is one series in Grafana Cloud
  instead of about 70 bucket series, which keeps staging inside the free tier's series limit.
  Assumption: Grafana Cloud stores them as native histograms; check after the first push.
- The Prometheus endpoint on the management port stays, for local use.
- **Cloud Run limit:** CPU is only allocated while a request is being handled. Between requests
  the push thread barely runs, so a quiet staging shows gaps, and a push can be late. Data is not
  lost: counters are cumulative. Always-allocated CPU would fix it and costs money; not worth it
  for staging.
- The retention job pushes once when it exits (the registry publishes on shutdown).
- Production export is decided with PROD-02.

## Consequences
- **Positive:**
  - Standard metrics in a standard format, private by construction on Cloud Run.
  - Local dashboards in one command.
- **Negative:**
  - The health path changes from `/actuator/health` to `/health`. External checks that used the
    old path must change; staging has no others today.
  - A second listening port: a little more memory, and one more thing to remember on other hosts.
    On a host that exposes every port, `MANAGEMENT_SERVER_PORT` must stay firewalled.
  - Staging collects metrics but sends them nowhere until PR 3.

## Alternatives considered
- **Keep the actuator on the main port and protect `/actuator/prometheus` with a role or token.**
  A scraper would need a long-lived credential, and one security rule would separate the metrics
  from the internet. Rejected.
- **OpenTelemetry SDK and OTLP only, no Prometheus endpoint.** Fine for export (PR 3), but there
  would be nothing to look at locally without a collector. Deferred to PR 3; Micrometer can do
  both.
- **Cloud Monitoring's built-in Cloud Run metrics only.** They cover requests, latency, CPU and
  memory at the container level, but nothing inside the JVM or the app. They are kept as a
  complement.
