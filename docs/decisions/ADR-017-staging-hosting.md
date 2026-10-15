# ADR-017: Staging hosting on Vercel, Cloud Run and Neon

## Status
Accepted (2026-10-15). The owner chose Google Cloud over AWS for staging; AWS practice comes
later. See the [free hosting spike](../product/spikes/free-hosting-spike.md).

## Date
2026-10-15

## Context
BrewDeck runs only locally. The owner wants a staging environment on the internet, at $0 per month
if possible. A proposal (Vercel + Render + Neon) was checked against the code and against the
providers' free tiers.

The spike found that:

- **Render Free cannot run the API.** On 0.1 CPU the API needs ~6.5 minutes to start. Vercel's
  external rewrites give up after 120 s, so every cold start would fail.
- **The browser must keep calling only the web origin** (ADR-013): Next proxies `/api/*`, and the
  refresh cookie is `SameSite=Strict`. A public API URL in the browser would break login refresh.
- **Memory is not the limit.** The API uses ~365 MiB after start under a 512 MiB limit.

## Decision

- **Web:** Vercel Hobby, root directory `brewdeck-web`.
  - Server-side `API_PROXY_TARGET` points at the API. `NEXT_PUBLIC_API_BASE_URL` stays empty.
  - Only the staging branch builds.
- **API:** Google Cloud Run, from a Docker image built in GitHub Actions.
  - Size and scaling: 1 vCPU, 512 MiB, startup CPU boost, `min-instances=0`, `max-instances=1`,
    request-based billing, a Tier 1 region.
  - Secrets live in Secret Manager.
  - GitHub authenticates with Workload Identity Federation, never with JSON keys.
- **Database:** Neon Free, direct (non-pooler) endpoint with `sslmode=require`, compute fixed at
  0.25 CU. HikariCP uses `minimum-idle: 0`, so Neon can scale to zero.
- **Spring profile:** a new `staging` profile, so feature flags resolve to their `staging` rows (all
  disabled). The AI assistant stays off: Ollama needs a large local model (ADR-016) and the
  Claude API costs money.
- **Email (password reset, verification):** a free external SMTP provider, no mail server of our
  own. Start with a Gmail account plus an app password (`smtp.gmail.com:587`, STARTTLS), stored in
  Secret Manager; Cloud Run only blocks port 25. Until it is set, `BREWDECK_MAIL_ENABLED=false` and
  reset links appear only in the Cloud Run logs. `auth-require-email-verification` stays off until
  mail works in staging.
- **Cost guardrails:** a $1 budget alert on the billing account, an Artifact Registry cleanup policy
  and `max-instances=1`.

## Consequences

- **Positive:**
  - The API starts in seconds; we measured 8 s at 1 vCPU.
  - No servers to patch.
  - The ADR-013 cookie model is unchanged.
  - Local Docker Compose development is unchanged.
- **Negative:**
  - A card is needed on a GCP billing account. Budget alerts do not stop spending.
  - Three providers to watch.
  - Cold starts of a few seconds after idle.
  - Free-tier terms can change.
- **Security:**
  - Per-IP auth rate limits act as one shared bucket, because `FORWARD_HEADERS_STRATEGY=none`: the
    `run.app` URL is public, so `X-Forwarded-For` cannot be trusted.
  - Registration is open.
  - Reset links are only logged until SMTP is set up.
  - Do not share the URL widely until an allowlist or verification is in place.

## Alternatives Considered

- **Vercel + Render + Neon** (the original proposal): rejected; startup on 0.1 CPU exceeds the proxy
  timeout.
- **Supabase instead of Neon:** rejected; projects pause after 7 idle days and there are no free
  backups.
- **Oracle Always Free VM with Docker Compose:** deferred. It is a good rehearsal for the home lab,
  but it means operations work and idle instances get reclaimed.
- **AWS (EC2 + Neon, Free plan):** deferred. A good way to practice AWS, but the Free plan
  closes the account after 6 months; Cloud Run's free tier does not expire. The same image (and
  Neon) can move to AWS later.
- **Railway, Koyeb, Fly.io:** rejected; no free compute that can run a JVM for a month.

## Deploy checklist (once accepted)

1. **Neon:**
   - Create project `brewdeck-staging`; choose a region close to the Cloud Run region.
   - Fix compute at 0.25 CU.
   - Copy the **direct** connection details and build
     `DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require`.
2. **GCP:**
   - Create a project and link billing.
   - Create a $1 budget alert.
   - Enable Cloud Run, Artifact Registry and Secret Manager.
   - Create a Docker repository with a cleanup policy that keeps the last 2 images.
3. **Secrets:**
   - Create a new `BREWDECK_JWT_SECRET`, e.g. `openssl rand -base64 48`.
   - Store it and `DB_PASSWORD` in Secret Manager.
4. **First deploy (manual):**
   ```bash
   gcloud run deploy brewdeck-api-staging \
     --image <region>-docker.pkg.dev/<project>/<repo>/brewdeck-api:<sha> \
     --region <tier-1 region> --cpu 1 --memory 512Mi --cpu-boost \
     --min-instances 0 --max-instances 1 --allow-unauthenticated \
     --set-env-vars SPRING_PROFILES_ACTIVE=staging,DB_URL=...,DB_USER=...,CORS_ALLOWED_ORIGINS=https://<vercel-url>,BREWDECK_MAIL_FRONTEND_BASE_URL=https://<vercel-url> \
     --set-secrets DB_PASSWORD=db-password:latest,BREWDECK_JWT_SECRET=jwt-secret:latest
   ```
   Then check:
   - `curl https://<service>.run.app/actuator/health` returns `{"status":"UP"}`.
   - The logs show 28 migrations applied.
5. **Vercel:**
   - Import the repository with root `brewdeck-web`.
   - Set the env var `API_PROXY_TARGET=https://<service>.run.app`, plus `ENABLE_EXPERIMENTAL_COREPACK=1`
     if pnpm 11 is not picked up.
   - Production branch: `develop`. Turn off preview deployments for other branches.
6. **Smoke test from desktop and phone:**
   - register → login → reload the page (refresh cookie) → coffee, recipe and session CRUD →
     logout;
   - then wait 20+ minutes and repeat (cold start).
7. **Automate:**
   - Add `deploy-api-staging.yml`, which runs after `api-ci` passes on `develop` (WIF; build, push,
     deploy).
   - Roll back by sending traffic to an earlier revision:
     `gcloud run services update-traffic --to-revisions <rev>=100`.
   - Flyway migrations only move forward. A rollback that needs an older schema means restoring
     Neon's point-in-time copy (6 h window).

## Notes
Free-tier numbers and sources are listed in the spike (checked 2026-10-15). Recheck them before
setting anything up.
