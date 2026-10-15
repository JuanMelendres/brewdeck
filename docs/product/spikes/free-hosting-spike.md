# Technical Spike: Free hosting for a BrewDeck staging environment

## 1. Summary

BrewDeck only runs locally today. The owner wants a staging environment on the internet, at $0
per month if possible, to test the full flow from real browsers and phones.

- **Question:** which free services can host the Next.js web app, the Spring Boot API, and
  PostgreSQL without changing the stack, and what must change in the repository first?
- **Input:** a proposal written with ChatGPT (2026-10-14) that recommends Vercel + Render + Neon. This
  spike checks that proposal against the code and against the providers' current free tiers.
- **Decision expected:** which architecture to use for staging, and which repository changes to make
  before the first deploy.

## 2. Background

- **Stack, from the repository, not the proposal:**
  - Spring Boot **4.1.1** on Java 21. The proposal says 3.x.
  - Next.js **16.4** with React 19 and pnpm 11.27.1 (`packageManager`), on Node ≥ 22.
  - PostgreSQL 16 with 28 Flyway migrations (V1–V28).
- **What exists for deployment:**
  - `docker-compose.yml` runs only Postgres and Mailpit.
  - There is no Dockerfile for the API or the web app.
  - There is no deploy workflow. CI covers tests, quality checks and security scans:
    `api-ci.yml`, `web-ci.yml`, `security.yml`, `qodana_code_quality.yml`.
  - The repository is **public**, so GitHub Actions minutes are free.
- **Production profile:** `application-prod.yml` exists. It requires `DB_URL`, `DB_USER`,
  `DB_PASSWORD` and `BREWDECK_JWT_SECRET`, and turns Swagger and the OpenAPI spec off.
  `/actuator/health` is public and hides details.
- **The proxy model (ADR-013) is the key constraint the proposal did not see.**
  - The browser only calls the web origin. `next.config.ts` rewrites `/api/*` to
    `API_PROXY_TARGET`.
  - The refresh token is an httpOnly cookie: `SameSite=Strict`, `Path=/api/auth`.
  - `NEXT_PUBLIC_API_BASE_URL` must stay **empty**. The proposal's `NEXT_PUBLIC_API_URL=https://<backend>`
    would make the cookie third-party and break login refresh.
  - CORS is not involved in the browser-to-API path.
- **Feature flags (ADR-007):** flags are scoped by environment. `EnvironmentResolver` derives the
  environment from the Spring profile and knows `staging`. Every flag already has `staging` rows,
  all disabled.
- **AI:** in staging the AI is either Ollama (local only, per ADR-016) or the paid Claude API. Both are
  out of scope: the `brew-recipe-ai-assistant` flag stays off outside `local`, as the owner asked.
- **Mail:** with `BREWDECK_MAIL_ENABLED=false`, email links are only logged.
  The `auth-require-email-verification` flag is off everywhere.
- **Previous decision:** "local only until the home lab exists" (2026-10-09). The owner changed this
  on 2026-10-15 and asked for this spike.

## 3. Problem Statement

We need a staging host for three parts — the Next.js server, a JVM API, and PostgreSQL — that:

- costs $0 within its quotas,
- keeps the first-party proxy and cookie model,
- runs Flyway on startup,
- deploys from GitHub,
- and still works after the service has been idle.

We also need to know whether the JVM even fits the free compute that each provider gives.

## 4. Goals

- Audit the repository for deployment readiness.
- Check each provider's current free tier against official documentation.
- Measure how long the API takes to start and how much memory it uses under free-tier CPU and memory
  limits.
- Recommend one architecture, the repository changes it needs, a CI/CD shape, and a deploy checklist.

## 5. Non-Goals

- Production hosting, a custom domain, or SLAs.
- AI in staging (the flag stays off; see ADR-016).
- Real email delivery (later, optional).
- Writing the Dockerfile, workflow or profile. Those belong to the implementation PRs (section 17).

## 6. Key Questions

- Q-001: Does the API start fast enough on each provider's free compute?
- Q-002: Does the proxy and cookie model work through Vercel's rewrites?
- Q-003: Which free tiers are permanent, which need a card, and which can bill us?
- Q-004: How do Flyway, HikariCP and Neon's scale-to-zero interact?
- Q-005: What must change in the repository, and what is optional?
- Q-006: What happens when staging sits idle for days?

## 7. Constraints

- $0 per month; a card on file is acceptable only with a hard cap or tight alerts.
- No stack change: Spring Boot 4, Next.js 16, PostgreSQL 16, Flyway.
- Local development stays on Docker Compose.
- Branch + PR workflow; secrets live only in the providers and GitHub.
- One person operates it; operations work should be low.

## 8. Options Considered

| Option | Web | API | DB | Pros | Cons | Complexity | Risk |
|---|---|---|---|---|---|---|---|
| A. Proposal | Vercel Hobby | Render Free | Neon Free | No card; easy setup | API starts in ~6.5 min on 0.1 CPU (Finding 1); Vercel's proxy gives up at 120 s; SMTP blocked | Low | **High: does not work** |
| B. Recommended | Vercel Hobby | Cloud Run (min 0, max 1) | Neon Free | 8 s start with 1 vCPU (2 with boost); Vercel and Neon need no card; free tier covers staging many times over | Needs a GCP billing account with a card; image registry and IAM setup | Medium | Low–Medium (billing) |
| C. Supabase DB | Vercel Hobby | Cloud Run | Supabase Free | Postgres with extras | Paused after 7 days idle; 500 MB; no backups; extras unused | Medium | Medium |
| D. One VM | Oracle A1 VM (or Vercel) | Same VM, Docker Compose | Same VM | Always on, no cold starts; 2 OCPU / 12 GB; same setup as the future home lab | Card required; idle VMs are reclaimed (staging is idle); we patch, back up, firewall and run TLS ourselves; ARM images; capacity can be "out of stock" | High | Medium–High |
| E. Railway | — | Railway Free ($1/month credit) | Railway or Neon | Simple deploys | $1 of credit does not run a JVM for a month | Low | High (stops) |
| F. Koyeb / Fly.io | — | — | — | — | No free compute for new accounts any more (Koyeb Pro only; Fly ended free allowances in Oct 2024) | — | Not viable |

## 9. Evaluation Criteria

| Criterion | Description | Weight |
|---|---|---|
| Works with this code | JVM starts within proxy limits; cookie and proxy model intact; Flyway runs | High |
| Cost | $0 within quotas; no surprise bill; card needed or not | High |
| Operations | Patching, TLS, backups, monitoring work for one person | High |
| Idle behavior | What a tester sees after hours or days of no traffic | Medium |
| Repository changes | How much code and config the provider forces | Medium |
| Path forward | Fits the later home lab or production | Low |

## 10. Research Findings

### Finding 1: Render Free cannot run this API behind Vercel (Q-001)

The API was measured in Docker with the real jar (114 MB), the `prod` profile, a throwaway
`postgres:16`, `--memory 512m` and `-XX:MaxRAMPercentage=75`:

| CPU limit | "Started … in" | Wall time | Memory after start | Notes |
|---|---|---|---|---|
| 1 | 8.5 s | 11 s | 365 MiB | First boot, 28 migrations applied |
| 1 | 7.3 s | 9 s | 362 MiB | Schema up to date |
| 0.5 | 33.8 s | 37 s | 365 MiB | |
| 0.1 | **381.7 s** | **411 s** | 363 MiB | Render Free's CPU |

Evidence:
- The measurements above, taken on 2026-10-15 on an Apple M4 host. The jar ran on arm64; Render's
  x86 hosts may differ, but not by 6×.
- Render documentation: Free services have 512 MB and 0.1 CPU. They spin down after 15 minutes
  without traffic and take "about one minute" to spin up (for small apps). Render blocks outbound
  SMTP ports 25, 465 and 587. Its free Postgres expires after 30 days.
- Vercel documentation: an external rewrite fails with `ROUTER_EXTERNAL_TARGET_ERROR` after
  **120 s**.

Impact:
- After every 15 minutes of idle time, the first request starts a ~6.5 min boot. Vercel's proxy
  gives up at 2 minutes, so the tester sees an error.
- Memory (≈365 MiB) is not the problem; CPU is.
- **Option A is rejected.**

### Finding 2: Cloud Run fits the measured profile (Q-001, Q-003)

- **Free tier:** with request-based billing, 180,000 vCPU-s, 360,000 GiB-s and 2 M requests per
  month, plus 1 GiB egress from North America. Tier 1 regions only (e.g. `us-central1`). The free
  tier belongs to the billing account.
- **Startup:** startup CPU boost gives a 1-vCPU service 2 vCPU while starting. We measured 8 s at
  1 vCPU, so a cold start is roughly 5–10 s plus Neon's wake-up.
- **Idle:** with `min-instances=0`, idle time is not billed.
- **Budget:** at 1 vCPU / 512 MiB, the free CPU covers ~50 h of active request time per month.
  Staging traffic will use a small fraction.
- **Image storage:** Artifact Registry gives 0.5 GB free. One JRE image is ~300 MB, so keep at most
  1–2 tags with a cleanup policy.
- **Cost risk:** Cloud Run needs a billing account with a card. Budget alerts **notify but do not
  stop** spending.
  - Mitigations: `max-instances=1`, a $1 budget alert, build in GitHub Actions instead of Cloud
    Build, and keep the registry small.
  - Optional: a budget → Pub/Sub → function that turns off billing.

Evidence: Cloud Run pricing and docs pages (CPU boost, billing settings, min instances), checked
2026-10-15.

### Finding 3: the cookie and proxy model works through Vercel, with one adjustment (Q-002)

- **Rewrites:** Next's `rewrites()` to an external URL become Vercel external rewrites, so the
  browser keeps talking only to the Vercel origin. The `brewdeck_refresh` cookie stays first-party
  and `SameSite=Strict` keeps working.
- **No CORS involved:** CORS does not apply to proxied calls. `CORS_ALLOWED_ORIGINS` can stay at the
  Vercel URL.
- **Timeouts:** `experimental.proxyTimeout: 100_000` only applies to `next start`. On Vercel, the
  limit is the platform's 120 s, which is enough for a Cloud Run cold start.
- **Client IP:** Vercel sends the visitor's IP in `x-forwarded-for`. But the API is also reachable
  directly at its `run.app` URL, so the header cannot be trusted. Keep
  `FORWARD_HEADERS_STRATEGY=none`. Then per-IP auth rate limits (ADR-011) act as one shared bucket;
  per-account limits still work.
- **Preview deployments:** Vercel builds one for every branch. If previews point at the same staging
  API, they all share one database. Limit builds to the staging branch, or turn previews off.
- **pnpm 11:** Vercel picks pnpm from the lockfile unless Corepack is on
  (`ENABLE_EXPERIMENTAL_COREPACK=1` plus the `packageManager` field). Users report rough edges with
  pnpm 11, so the first deploy must confirm the pnpm version used, or pin the install command.
  **Result of the first deploy (2026-10-15):** Corepack did not engage. Vercel looks for
  `packageManager` in the repository-root `package.json`, which this monorepo lacks. Pinning the
  install and build commands to `npx pnpm@11.27.1` fixed it (runbook step 6.1).

Assumption: Vercel passes `Set-Cookie` from external rewrites through unchanged. This is standard
proxy behavior, but the POC must confirm it.

### Finding 4: Neon is the best free database for idle staging (Q-003, Q-004, Q-006)

**Neon Free, no card:**
- 1 GB storage per project (20 GB per account). Writes are blocked above it; nothing is deleted.
- 100 CU-hours per project per month.
- 5 GB egress.
- Scale-to-zero after 5 minutes; this cannot be turned off.
- 6 h point-in-time restore and one manual snapshot.

**When a limit is hit:**
- Storage limit: writes are blocked.
- CU-hours or egress limit: compute is suspended until next month.
- Nothing is billed.

**Flyway:** use the **direct** endpoint (no `-pooler` suffix). PgBouncer in transaction mode breaks
session-level statements that migration tools use. With one API instance and a small pool, the app
can use the direct endpoint for everything.

**HikariCP and scale-to-zero:**
- Neon only sleeps when no connections are open.
- Hikari's defaults keep 10 open, so set `minimum-idle: 0` and a short `idle-timeout`. Then Neon can
  sleep while a Cloud Run instance is still warm. When Cloud Run scales to zero, the connections
  close anyway.
- With compute fixed at 0.25 CU, 100 CU-h gives ~400 h of awake time per month.

**Supabase Free is worse for idle staging:** it pauses the project after 7 days without activity
(resume is manual), has 500 MB and no backups.

**Render Postgres Free** expires after 30 days.

### Finding 5: the Oracle VM is powerful but is operations work and gets reclaimed (Q-003, Q-006)

**What Always Free gives:**
- Ampere A1: **2 OCPU / 12 GB** in total. Oracle's documentation now states this; older posts say 4
  OCPU / 24 GB.
- 200 GB of block storage and 10 TB of egress.
- Two AMD micro VMs with 1 GB each.

**Idle reclamation:** Oracle reclaims an instance when, over 7 days, CPU, network and memory (A1
only) all stay under 20% at the 95th percentile. A quiet staging VM fits that profile. Avoiding it
means upgrading to Pay As You Go, which brings back billing risk.

**Card:** a card and a phone number are required at sign-up.

**Fit:** it would be the closest rehearsal for the home lab (Compose plus a reverse proxy). Still, a
one-person staging should not start with OS patching, TLS, backups and firewall work.

### Finding 6: the repository needs a few changes before any deploy (Q-005)

**Required:**
1. **API Dockerfile** (`brewdeck-api/Dockerfile` + `.dockerignore`):
   - Multi-stage: build with `./mvnw -DskipTests package`, run on `eclipse-temurin:21-jre`.
   - Non-root user and `-XX:MaxRAMPercentage=75`.
   - `SERVER_PORT` from Cloud Run's `PORT` (8080 by default, already matching).
2. **`application-staging.yml`**, so `EnvironmentResolver` resolves `staging` without an override.
   It should contain:
   - the same required variables as `prod` (datasource and JWT secret; docs off);
   - Hikari `maximum-pool-size: 5`, `minimum-idle: 0`, `idle-timeout: 60000`;
   - `BREWDECK_LEGACY_TIMEZONE` left at `UTC` (fresh database).
3. **Staging environment variables:**
   - Set: `SPRING_PROFILES_ACTIVE=staging`, `DB_URL`, `DB_USER`, `DB_PASSWORD`,
     `BREWDECK_JWT_SECRET` (new, ≥ 32 bytes, from Secret Manager), `CORS_ALLOWED_ORIGINS=<vercel url>`,
     `BREWDECK_MAIL_FRONTEND_BASE_URL=<vercel url>`.
   - Unset: `AI_ENABLED`, `BREWDECK_MAIL_ENABLED`, `FORWARD_HEADERS_STRATEGY`.
   - `DB_URL` format: `jdbc:postgresql://<direct-host>/<db>?sslmode=require`.
4. **Web on Vercel:** set the root directory to `brewdeck-web` and only server-side
   `API_PROXY_TARGET=https://<service>.run.app`. Leave `NEXT_PUBLIC_API_BASE_URL` empty.
5. **Deploy workflow** `deploy-api-staging.yml`. Auth to GCP with Workload Identity Federation (no
   JSON keys). It builds the image, pushes it to Artifact Registry and runs `gcloud run deploy`,
   only after `api-ci` passes on the staging branch.

**Optional:**
- Docs: add a staging section to `environment-variables.md`, and update `setup.md` with how to run the
  image locally.
- Startup: Spring Boot CDS or a layered jar to cut cold starts further.
- A smoke-test step after deploy: `curl /actuator/health`, then a login.
- A shared-secret header from Next to the API, so the `run.app` URL cannot be used directly.
- Free SMTP (Brevo or Resend free tiers), to test password reset and verification for real.

## 11. Proof of Concept Plan

**Done in this spike:** the startup and memory measurement in Finding 1.
- Time-box: 30 min.
- The containers and network were removed afterwards.

**Next POC, part of the implementation PRs, time-boxed to ~2 h:**
1. Build the Dockerfile and run it locally against Compose Postgres with `SPRING_PROFILES_ACTIVE=staging`.
2. Create Neon; deploy to Cloud Run by hand once (`gcloud run deploy --source` or a pushed image).
   Check `/actuator/health`, the 28 migrations and the cold start after 15+ min idle.
3. Import the web app to Vercel with `API_PROXY_TARGET`. Check register → login → reload (refresh
   cookie) → CRUD → logout, from a phone.
4. Only then automate it with the workflow.

## 12. Proof of Concept Results

| What was tested | Result |
|---|---|
| Memory after start | ≈ 365 MiB with a 512 MiB limit at every CPU level. 512 MiB fits; there was no OOM. |
| First boot on an empty database | 28 migrations applied, then started in 8.5 s (1 CPU) |
| Startup vs CPU | 8.5 s at 1 CPU, 34 s at 0.5, 382 s at 0.1 |

**Not tested yet:**
- Vercel ↔ Cloud Run cookie flow.
- Neon latency.
- Real Cloud Run cold start.
- pnpm 11 on Vercel.

## 13. Trade-Off Analysis

| Trade-Off | Benefit | Cost |
|---|---|---|
| Cloud Run (card) vs Render (no card) | The API actually starts; 2–10 s cold starts | Billing account with a card; must cap and watch |
| Managed (Vercel + Cloud Run + Neon) vs one Oracle VM | No servers to run; scale-to-zero | Three providers and three dashboards; cold starts |
| Neon vs Supabase | Wakes by itself; no weekly pause | Fewer extras (unused anyway) |
| Direct Neon endpoint vs pooler | Flyway works; simpler | Fewer max connections (fine with one instance) |
| Keep `FORWARD_HEADERS_STRATEGY=none` | No IP spoofing | Per-IP limits are global in staging |

## 14. Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| GCP bill above $0 | Money | Low | `max-instances=1`, min 0, $1 budget alert, GitHub Actions builds, registry cleanup policy; optionally auto-disable billing |
| Free-tier terms change | Staging stops or starts billing | Medium (has happened at Fly, Koyeb, Render DB, Oracle A1) | Keep the app container-portable; one deploy doc; recheck quarterly |
| Open registration on a public URL | Strangers create accounts; abuse | Medium | Don't publish the URL; rate limits exist; later an invite flag or turn verification on with SMTP |
| Reset links written to logs | Anyone with log access can reset a password | Low (only the owner has access) | Staging only; enable SMTP before outside testers |
| Shared per-IP rate-limit bucket | One abuser locks login for everyone | Low | Per-account limits remain; a trusted-proxy follow-up if needed |
| Vercel previews hit staging data | Test branches write to staging | Medium | Limit Vercel builds to the staging branch |
| Neon CU-hours or egress run out | DB suspended until next month | Low | Fixed 0.25 CU, `minimum-idle: 0`, watch the usage page |
| pnpm 11 on Vercel | Build fails | Happened | Pinned install and build commands (`npx pnpm@11.27.1`); Corepack does not work without a root `package.json` |
| Vercel Hobby is personal and non-commercial | Must move to a paid plan before any business use | Low now | Revisit at production time |
| Secrets in a public repo | Leak | Low | Secrets only in Secret Manager, Vercel and GitHub; WIF instead of keys |

## 15. Recommendation

**Recommended option: B — Vercel Hobby (web) + Cloud Run (API) + Neon Free (Postgres), deployed
from `master` (releases only, decided 2026-10-15).**

- **Why B:**
  - It is the only all-managed option where the measured JVM startup (8 s at 1 vCPU) fits Vercel's
    120 s proxy limit.
  - It keeps ADR-013's proxy and cookie model untouched.
  - It costs $0 in practice: staging uses a tiny share of Cloud Run's free tier, and Vercel and Neon
    stop at their limits instead of billing.
- **Why not A (the proposal):** Render Free's 0.1 CPU makes Spring Boot start in ~6.5 min. The
  proposal also suggested `NEXT_PUBLIC_API_URL` pointing at the backend, which would break the
  refresh cookie.
- **Why not C:** Supabase pauses idle projects weekly.
- **Why not D:** too much operations work and reclaim risk for staging. Keep it as the rehearsal for
  the home lab later.
- **Why not E/F:** not free for a JVM, or not free at all.
- **Supporting assumptions:** the owner accepts a card on a GCP billing account with caps and alerts;
  staging traffic stays light; Vercel passes `Set-Cookie` through rewrites.
- **Validate next:** the POC in section 11, especially the real cold start and the cookie flow from
  a phone.

**What will stop being free first:** Neon CU-hours, if connections are left open, and Vercel Active
CPU (4 h per month) if pages render heavily on the server. Cloud Run's free tier has the most room,
but it is the only one that can bill.

## 16. Decision

- Decision: Option B (Vercel Hobby + Cloud Run + Neon Free). AWS was considered as a learning
  option (Free plan: $100–200 credits, account closes after 6 months) and deferred.
- Email: free external SMTP (Gmail app password first); no own mail server.
- AI: stays off in staging.
- Date: 2026-10-15
- Owner: Juan
- Status: Accepted

## 17. Next Steps

If accepted, as separate PRs:

1. ~~`feat(api)`: Dockerfile + `.dockerignore` + `application-staging.yml`~~ (done, #241).
2. Manual first deploy (owner), following the
   [staging deployment runbook](../../development/staging-deployment.md): Neon, Gmail SMTP, Google
   Cloud (billing guardrails, Artifact Registry, Secret Manager, Cloud Run), Vercel with production
   branch `master`.
3. `ci`: `deploy-api-staging.yml` (WIF; runs when `master` changes and `api-ci` passes).
4. Smoke tests from desktop and phone; update project state.
5. Follow-ups: turn `auth-require-email-verification` on in staging once mail works; Playwright
   smoke tests.

## 18. ADR Candidate

- ADR needed: Yes.
- Suggested title: ADR-017, staging hosting on Vercel, Cloud Run and Neon.
- Reason: a deployment and infrastructure decision with cost and security consequences. Written as
  **Proposed** alongside this spike.

## 19. Open Questions

- ~~OQ-001: Is a card on a GCP billing account acceptable?~~ Yes (2026-10-15): Google Cloud chosen.
- ~~OQ-002: Deploy staging from `develop` or from `master`?~~ `master` (releases only), 2026-10-15.
- ~~OQ-003: Open registration or an allowlist?~~ Open registration, 2026-10-15.
- OQ-004: Which Neon region best matches `us-central1`? Measure latency in the POC.

## 20. Assumptions

- Assumption-001: free-tier numbers are as published on 2026-10-15 and can change.
- Assumption-002: Docker timings on an M4 Mac roughly match a 1 vCPU Cloud Run instance
  (startup CPU boost should make Cloud Run faster).
- Assumption-003: staging serves a handful of testers, not public traffic.
- Assumption-004: AI stays disabled in staging until the owner decides otherwise.

## Sources (checked 2026-10-15)

- Render Free: https://render.com/docs/free; instance size: https://render.com/docs/compute-plans
- Vercel pricing: https://vercel.com/pricing; external rewrites (120 s): https://vercel.com/docs/routing/rewrites;
  limits: https://vercel.com/docs/limits; Corepack: https://vercel.com/changelog/corepack-experimental-is-now-available
- Neon pricing: https://neon.com/pricing; pooling and Flyway: https://neon.com/docs/connect/connection-pooling
- Cloud Run pricing: https://cloud.google.com/run/pricing; CPU boost: https://docs.cloud.google.com/run/docs/configuring/services/cpu
- Supabase pricing: https://supabase.com/pricing
- Oracle Always Free: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Koyeb pricing: https://www.koyeb.com/pricing
- Fly.io and Railway: third-party summaries of current pricing (fly.io/docs/about/pricing and
  railway.com/pricing were not fetched); treat as Assumption until checked.
