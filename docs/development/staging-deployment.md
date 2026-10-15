# Staging Deployment (manual runbook)

Step-by-step guide to create BrewDeck's staging environment by hand. The design and its reasons
are in [ADR-017](../decisions/ADR-017-staging-hosting.md) and the
[free hosting spike](../product/spikes/free-hosting-spike.md).

| Part | Service | Free limit (checked 2026-10-15) |
|---|---|---|
| Web (Next.js) | Vercel Hobby | Personal, non-commercial use; stops at its limits instead of billing |
| API (Spring Boot) | Google Cloud Run, `us-central1` | 180,000 vCPU-s, 360,000 GiB-s and 2 M requests per month |
| Database | Neon Free, AWS `us-east-2` | 100 CU-hours per month, 1 GB storage, scales to zero after 5 min |
| Email | Gmail with an app password | About 500 emails per day |
| Images | Artifact Registry | 0.5 GB |
| Secrets | Secret Manager | 6 active secret versions, 10,000 access operations per month |

**Rules for this environment:**

- Staging deploys from **`master`** only, so it changes when a release is merged.
- Registration is **open**.
- The AI assistant stays **off**: the `brew-recipe-ai-assistant` flag is off in `staging` and
  `AI_ENABLED` defaults to `false`.
- Secrets never go in the repository, in this document, or in shell history (the steps below use
  `read -s`).

Do the steps in order. Each step lists what you will need later. Write those values in a private
note (a password manager is best). **Do not commit them.**

## 0. Before you start

You need:

- A Google account for Google Cloud and a card for its billing account.
- A GitHub login for Neon and Vercel.
- A separate Gmail account for sending mail (step 2).
- On your Mac:
  - **Google Cloud CLI:** `brew install --cask google-cloud-sdk`, then `gcloud init`.
  - **Docker Desktop:** running.
  - **`openssl`:** already on macOS.
- A local checkout of `master`:

  ```bash
  git checkout master && git pull
  ```

**Alternative: Google Cloud Shell.** Instead of your Mac, you can run every `gcloud`, `docker` and
`openssl` command in Cloud Shell, the terminal that opens from the `>_` icon in the Cloud Console.

- It already has `gcloud` (logged in), Docker, `git` and `openssl`, so `gcloud auth login` is not
  needed there.
- It runs on **amd64**, like Cloud Run, so the image builds natively. On Apple Silicon the build
  goes through emulation and is slower.
- Its `$HOME` (5 GB) is kept between sessions, but only the files are kept. Exported variables are
  lost, as on any terminal (see below).
- It is a different machine from your Mac. Create `~/brewdeck-staging/env.sh` there too, and clone
  the repository there:

  ```bash
  git clone https://github.com/JuanMelendres/brewdeck.git ~/brewdeck
  cd ~/brewdeck && git checkout master && git pull
  ```

Whichever terminal you use, run the commands **from the repository root**. Steps 4.2 and 9 use
`git rev-parse` for the image tag.

### Shell variables for every terminal

The commands below use `$GCP_PROJECT`, `$REGION` and `$RUNTIME_SA`. An `export` only lasts in the
terminal where you ran it. In a new terminal they are empty, and `gcloud` fails with errors like
`Failed to find attribute [location]`.

So keep them in a file **outside the repository**. These are not secrets.

```bash
mkdir -p ~/brewdeck-staging
echo 'export REGION=us-central1' > ~/brewdeck-staging/env.sh
source ~/brewdeck-staging/env.sh
```

Step 3.2 adds the project id to this file.

> **Every time you open a new terminal for staging, run `source ~/brewdeck-staging/env.sh` first.**
> Check it worked with `echo "$GCP_PROJECT $REGION"`; neither value should be empty.

Keep this private note from now on:

```text
GCP_PROJECT=
REGION=us-central1
NEON_HOST=
NEON_DB=brewdeck
NEON_USER=
MAIL_USER=
API_URL=
WEB_URL=
```

## 1. Neon (PostgreSQL)

1. Go to https://neon.com and **Sign up** with GitHub. Choose the **Free** plan; no card is
   asked for.
2. **Create project:**
   - Project name: `brewdeck-staging`
   - Postgres version: **16** (same as local and CI)
   - Cloud provider and region: **AWS, US East 2 (Ohio)**, the closest Neon region to Cloud Run's
     `us-central1`
   - Database name: `brewdeck`
3. Open **Branches → the default branch (`production`, or `main` in older projects) → Computes → Edit**:
   - Set autoscaling to **0.25 CU min and 0.25 CU max**. This keeps CU-hours low: 100 CU-h is
     about 400 h of awake time.
   - Leave **scale to zero** on (5 minutes).
4. Click **Connect** on the project dashboard:
   - Database: `brewdeck`.
   - Turn **Connection pooling OFF**. The host must **not** contain `-pooler`, because Flyway needs
     a direct connection.
   - Copy the host (`ep-…us-east-2.aws.neon.tech`), the role (user) and the password.
5. Optional: check the connection from your Mac (`brew install libpq` gives you `psql`):

   ```bash
   psql "postgresql://<NEON_USER>@<NEON_HOST>/brewdeck?sslmode=require" -c 'select version();'
   ```

The tables are created later by Flyway when the API starts for the first time. **Do not create
them by hand.**

**Save:** `NEON_HOST`, `NEON_USER`, and the password (only in your password manager).

## 2. Gmail (outgoing mail)

Use a **dedicated** account, for example `brewdeck.staging@gmail.com`, not your personal one. The
app password gives full mail access to that account.

1. Create the Gmail account and sign in.
2. Go to https://myaccount.google.com/security and turn on **2-Step Verification**. App passwords
   need it.
3. Go to https://myaccount.google.com/apppasswords and create an app password named
   `BrewDeck staging`.
4. Google shows a 16-character code. Copy it **without spaces**. It is shown only once.

**Save:** `MAIL_USER` (the Gmail address) and the app password (password manager only).

The SMTP settings used in step 5 are:

- `smtp.gmail.com`, port `587`, STARTTLS.
- The `From` must be this same Gmail address.

Cloud Run only blocks port 25, so 587 works.

## 3. Google Cloud project, billing and guardrails

### 3.1 Billing account

1. Go to https://console.cloud.google.com and accept the terms.
2. Start the free trial ($300 for 90 days) and add your card. During the trial nothing is charged.
3. **Before the trial ends:**
   - Set a calendar reminder for day 80.
   - On that day, open **Billing → Overview** and click **Activate full account**. If you don't,
     the project stops when the trial ends.
   - Cloud Run's monthly free tier keeps applying after activation.

### 3.2 Project

```bash
gcloud auth login
source ~/brewdeck-staging/env.sh
export GCP_PROJECT=brewdeck-staging-$RANDOM    # project IDs are global; keep the one that works
gcloud projects create "$GCP_PROJECT" --name="BrewDeck staging"

# Only after the create succeeded: save the id and the runtime account used in step 5.2.
echo "export GCP_PROJECT=$GCP_PROJECT" >> ~/brewdeck-staging/env.sh
echo 'export RUNTIME_SA="brewdeck-api-runtime@$GCP_PROJECT.iam.gserviceaccount.com"' >> ~/brewdeck-staging/env.sh
source ~/brewdeck-staging/env.sh

gcloud config set project "$GCP_PROJECT"
gcloud config set run/region "$REGION"
gcloud config set artifacts/location "$REGION"

gcloud billing accounts list                   # copy the ACCOUNT_ID
gcloud billing projects link "$GCP_PROJECT" --billing-account=<ACCOUNT_ID>
```

### 3.3 Budget alert

In the console, open **Billing → Budgets & alerts → Create budget**:

- Name: `brewdeck-staging`.
- Scope: project `brewdeck-staging-…`.
- Amount: the smallest amount in your billing currency, for example **USD 1 or MXN 20**.
- Thresholds: 50% and 100% of actual spend, plus 100% of forecasted spend.
- Email alerts to billing admins: on.

> Budget alerts **only notify**; they do not stop spending. The real caps are `--max-instances=1`
> (step 5) and the small image registry.

### 3.4 APIs

```bash
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com
```

## 4. Image registry and the first image

### 4.1 Artifact Registry with a cleanup policy

```bash
gcloud artifacts repositories create brewdeck \
  --repository-format=docker --location="$REGION" --description="BrewDeck images"

cat > /tmp/brewdeck-cleanup.json <<'EOF'
[
  {"name": "keep-latest-2", "action": {"type": "Keep"}, "mostRecentVersions": {"keepCount": 2}},
  {"name": "delete-older", "action": {"type": "Delete"}, "condition": {"olderThan": "1d"}}
]
EOF
gcloud artifacts repositories set-cleanup-policies brewdeck \
  --location="$REGION" --policy=/tmp/brewdeck-cleanup.json --no-dry-run
```

Each image is about 206 MB compressed. Keeping 2 stays under the 0.5 GB free storage.

### 4.2 Build and push

Cloud Run runs **amd64**. Run this from the repository root, on `master`:

```bash
source ~/brewdeck-staging/env.sh
cd ~/brewdeck            # or wherever your checkout is
git checkout master && git pull
gcloud auth configure-docker "$REGION-docker.pkg.dev"

export IMAGE="$REGION-docker.pkg.dev/$GCP_PROJECT/brewdeck/brewdeck-api:$(git rev-parse --short HEAD)"
echo "$IMAGE"            # must end in :<commit>, not in a bare ':'
```

Then build and push, depending on where you are:

- **Mac (Apple Silicon):** build for amd64 explicitly. Maven runs under emulation, so the first
  build takes several minutes. A plain `docker build` on the Mac makes an **arm64** image, which
  Cloud Run rejects. `--provenance=false` skips the attestation, so the registry gets a single amd64
  manifest instead of an index.

  ```bash
  docker buildx build --platform linux/amd64 --provenance=false -t "$IMAGE" --push brewdeck-api
  ```

- **Cloud Shell:** it is already amd64.

  ```bash
  docker build -t "$IMAGE" brewdeck-api && docker push "$IMAGE"
  ```

Check the platform before deploying:

```bash
docker buildx imagetools inspect "$IMAGE"    # must show Platform: linux/amd64
```

If it shows `linux/arm64`, rebuild with the Mac command above, using the same tag. Otherwise
step 5.4 fails with `Container manifest type 'application/vnd.oci.image.index.v1+json' must
support amd64/linux`.

Notes:

- `gcloud auth configure-docker` may print a long `WARNING` listing `credHelpers`. That is
  informational; the line that matters is `gcloud credential helpers already registered correctly`.
- If the tag ends in a bare `:` and the build fails with `invalid reference format`, you are not
  inside the repository. `git rev-parse` failed with `fatal: not a git repository`.
- `brewdeck-api/Dockerfile` must exist on `master`, so the release with #241 must be merged first.

## 5. API on Cloud Run

### 5.1 Secrets

`read -s` keeps the values out of your terminal and shell history.

```bash
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create brewdeck-jwt-secret --data-file=-

read -rs DB_PASS && printf '%s' "$DB_PASS" | gcloud secrets create brewdeck-db-password --data-file=- && unset DB_PASS
read -rs MAIL_PASS && printf '%s' "$MAIL_PASS" | gcloud secrets create brewdeck-mail-password --data-file=- && unset MAIL_PASS
```

The first `read` waits for the Neon password; the second waits for the Gmail app password.

### 5.2 Runtime service account (least privilege)

The default Compute service account has the broad Editor role, so the API gets its own account.
It can only read these three secrets.

```bash
gcloud iam service-accounts create brewdeck-api-runtime --display-name="BrewDeck API runtime"
echo "$RUNTIME_SA"    # set by env.sh in step 3.2; must not be empty

for s in brewdeck-jwt-secret brewdeck-db-password brewdeck-mail-password; do
  gcloud secrets add-iam-policy-binding "$s" \
    --member="serviceAccount:$RUNTIME_SA" --role=roles/secretmanager.secretAccessor
done
```

### 5.3 Environment file

`DB_URL` contains `?`, which is awkward on the command line, so use a YAML file **outside the
repository**:

```bash
cat > ~/brewdeck-staging/api.env.yaml <<'EOF'
SPRING_PROFILES_ACTIVE: "staging"
DB_URL: "jdbc:postgresql://<NEON_HOST>/brewdeck?sslmode=require"
DB_USER: "<NEON_USER>"
# Replaced with the real Vercel URL in step 6.4.
CORS_ALLOWED_ORIGINS: "https://brewdeck-staging.vercel.app"
BREWDECK_MAIL_FRONTEND_BASE_URL: "https://brewdeck-staging.vercel.app"
BREWDECK_MAIL_ENABLED: "true"
BREWDECK_MAIL_FROM: "BrewDeck <MAIL_USER>"
SPRING_MAIL_HOST: "smtp.gmail.com"
SPRING_MAIL_PORT: "587"
SPRING_MAIL_USERNAME: "<MAIL_USER>"
SPRING_MAIL_SMTP_AUTH: "true"
SPRING_MAIL_SMTP_STARTTLS: "true"
EOF
```

Replace every `<…>` placeholder. Leave these out on purpose:

- `AI_ENABLED`: defaults to `false`.
- `FORWARD_HEADERS_STRATEGY`: defaults to `none`, so a client cannot spoof its IP.
- `API_DOCS_ENABLED`: defaults to `false`.

### 5.4 Deploy

```bash
gcloud run deploy brewdeck-api-staging \
  --image="$IMAGE" \
  --region="$REGION" \
  --service-account="$RUNTIME_SA" \
  --cpu=1 --memory=512Mi --cpu-boost \
  --min-instances=0 --max-instances=1 \
  --allow-unauthenticated \
  --env-vars-file="$HOME/brewdeck-staging/api.env.yaml" \
  --set-secrets=DB_PASSWORD=brewdeck-db-password:latest,BREWDECK_JWT_SECRET=brewdeck-jwt-secret:latest,SPRING_MAIL_PASSWORD=brewdeck-mail-password:latest
```

What the settings mean:

- `--allow-unauthenticated` is needed because Vercel calls the API like any browser would. The app
  still requires a JWT for every `/api/**` route except the public ones.
- `--min-instances=0` with the default request-based billing means idle time costs nothing.
- `--max-instances=1` is the cost cap. It also keeps the in-memory rate limits and caches on a
  single instance.
- Cloud Run sets `PORT=8080`, and the `staging` profile reads it.

### 5.5 Check

```bash
export API_URL=$(gcloud run services describe brewdeck-api-staging --format='value(status.url)')
curl -s "$API_URL/actuator/health"          # {"groups":["liveness","readiness"],"status":"UP"}
curl -s -o /dev/null -w '%{http_code}\n' "$API_URL/api/coffees"   # 401 (needs login)

gcloud logging read \
  'resource.type="cloud_run_revision" AND resource.labels.service_name="brewdeck-api-staging"' \
  --limit=200 --format='value(textPayload)' | grep -E 'Successfully applied|Started Brewdeck|ERROR'
```

Right after a deploy, or after the service has been idle, the health check may answer
`{"status":"OUT_OF_SERVICE"}` for a few seconds. That is Spring's readiness state while the instance
is still starting: Tomcat already listens, but Flyway and the context are not done yet. Repeat the
`curl` after 10–20 s. Only a `DOWN`, or an `OUT_OF_SERVICE` that does not go away, needs the logs.

The first start against Neon should log `Successfully applied 28 migrations` (or the current
count) and `Started BrewdeckApiApplication`.

If it fails:

| Error in the logs | Cause and fix |
|---|---|
| `Could not resolve placeholder` | An env var or secret is missing. |
| `password authentication failed` | Check `DB_USER` and the secret value. |
| `SSL` errors | Check `sslmode=require`. |
| Flyway errors with `-pooler` in the host | Use the direct endpoint (step 1.4). |

**Save:** `API_URL` (`https://brewdeck-api-staging-….run.app`).

## 6. Web on Vercel

### 6.1 Import the project

1. Go to https://vercel.com, **Sign up** with GitHub, and choose **Hobby**.
2. Click **Add New → Project**.
3. Install the Vercel GitHub app for **only** the `JuanMelendres/brewdeck` repository, then
   **Import** it.
4. Configure:
   - **Project name:** `brewdeck-staging`. This gives `https://brewdeck-staging.vercel.app` if the
     name is free; otherwise note the URL Vercel assigns.
   - **Framework preset:** Next.js.
   - **Root directory:** `brewdeck-web`.
   - **Build and Output Settings:** turn **Override** on for both commands:

     | Field | Value |
     |---|---|
     | Install Command | `npx pnpm@11.27.1 install --frozen-lockfile` |
     | Build Command | `npx pnpm@11.27.1 run build` |

     Vercel's own pnpm is too old for our lockfile. It warns `Ignoring not compatible lockfile`,
     then fails with `ERR_INVALID_THIS`. Corepack (`ENABLE_EXPERIMENTAL_COREPACK=1`) does not help
     either: Vercel looks for `packageManager` in the **repository root** `package.json`, which this
     monorepo does not have (`Could not enable corepack because package.json is missing
     "packageManager"`). Keep the version in sync with `brewdeck-web/package.json`.
   - **Environment variables** (Production):

     | Name | Value | Notes |
     |---|---|---|
     | `API_PROXY_TARGET` | your `API_URL`, no trailing slash | Read at **build** time by `next.config.ts` |

     **Do not** set `NEXT_PUBLIC_API_BASE_URL`. The browser must only talk to the Vercel origin,
     otherwise the refresh cookie breaks ([ADR-013](../decisions/ADR-013-refresh-token-cookie.md)).
5. Click **Deploy**. This first build uses the repository's default branch (`develop`); step 6.2
   switches it to `master`.

### 6.2 Deploy from `master` only

In **Project → Settings**:

1. **Git → Production Branch:** `master`.
2. **Build and Deployment → Ignored Build Step:** choose **Only build production**, so pushes to
   other branches don't create preview builds.
3. **Build & Deployment → Node.js Version:** **22.x** (`package.json` asks for Node ≥ 22).

Then rebuild from `master`: **Deployments → Create Deployment**, branch `master` (or merge
anything into `master`). Check that the production deployment shows branch `master`.

### 6.3 Check the build log

- The install step must **not** show `Ignoring not compatible lockfile` or `ERR_INVALID_THIS`.
  If it does, the overrides from step 6.1 are missing.
- The `Ignored Build Step` line (`if [ "$VERCEL_ENV" == "production" ]; then exit 1; ...`) exiting
  with 1 is correct. It means "build this one", because production builds.
- After changing settings or env vars, use **Deployments → … → Redeploy** with **Use existing
  Build Cache** unchecked. Settings only apply to new builds.

**Save:** `WEB_URL` (the production domain, for example `https://brewdeck-staging.vercel.app`).

### 6.4 Point the API at the real web URL

If `WEB_URL` differs from the placeholder in step 5.3, update both URLs. Also edit them in
`~/brewdeck-staging/api.env.yaml`, so the file stays the record of what runs:

```bash
gcloud run services update brewdeck-api-staging \
  --update-env-vars=CORS_ALLOWED_ORIGINS=<WEB_URL>,BREWDECK_MAIL_FRONTEND_BASE_URL=<WEB_URL>
```

`--update-env-vars` changes only the listed variables; the others and the secrets stay. The update
creates a new revision.

## 7. Smoke test (desktop and phone)

1. **Register:** open `WEB_URL` and register a new account. Registration is open.
2. **Refresh cookie:** log in, then reload the page. You should stay logged in.
3. **Data:**
   - Create a coffee, a recipe and a brew session.
   - Check the dashboard.
   - Switch to Spanish and change the theme.
4. **Password reset:**
   - Log out, then click **Forgot password**.
   - The email should arrive at the address you registered with (check spam).
   - Follow the link: it must open `WEB_URL`. Reset the password and log in with the new one.
5. **AI:** the AI buttons should not show (flag off).
6. **Cold start:** wait **20+ minutes** and load the app again.
   - The first request takes a few extra seconds while Cloud Run and Neon wake up.
   - It should not fail.
7. **Phone:** repeat steps 2–4 on your phone, and try **Add to Home screen**.

## 8. Optional: admin account

1. Register the account you want as admin.
2. Set it, and add the same line to `~/brewdeck-staging/api.env.yaml`:

   ```bash
   gcloud run services update brewdeck-api-staging --update-env-vars=BREWDECK_ADMIN_EMAIL=<that email>
   ```

3. The update creates a new revision. The new revision
   promotes the account at startup ([ADR-009](../decisions/ADR-009-role-based-authorization.md)).

## 9. Updating staging by hand (until the deploy pipeline exists)

Staging follows `master`.

1. Merge the release PR `develop → master`.
2. **Web:** Vercel deploys `master` by itself, so nothing to do.
3. **API:** build and deploy the new commit.

   ```bash
   source ~/brewdeck-staging/env.sh
   cd ~/brewdeck && git checkout master && git pull    # your checkout, on the Mac or in Cloud Shell
   export IMAGE="$REGION-docker.pkg.dev/$GCP_PROJECT/brewdeck/brewdeck-api:$(git rev-parse --short HEAD)"
   docker buildx build --platform linux/amd64 -t "$IMAGE" --push brewdeck-api
   gcloud run deploy brewdeck-api-staging --image="$IMAGE" --region="$REGION"
   ```

   A redeploy with only `--image` keeps the env vars, secrets and limits.

**Order when a release has new migrations:**

- Deploy the **API first**. Flyway runs on startup, and the web keeps working because API changes
  are additive.
- If a release removes something the old web uses (like #237's `ui-config`), deploy both close
  together.

**Rollback:**

- **API:** list the revisions, then send all traffic to an earlier one.

  ```bash
  gcloud run revisions list --service=brewdeck-api-staging
  gcloud run services update-traffic brewdeck-api-staging --to-revisions=<REVISION>=100
  ```

  The next `gcloud run deploy` sends traffic to the newest revision again.
- **Web:** Vercel **Deployments → … → Instant Rollback** on an earlier production deployment.
- **Database:** Flyway only moves forward. If a release needs the old schema back, use Neon
  **Restore** (6-hour window on Free) to a point before the deploy, then roll back the API.

## 10. Watching cost and usage

Check monthly, or when a budget email arrives:

- **Google Cloud:**
  - **Billing → Reports**, filtered to the project. It should show $0 after credits and the free
    tier.
  - **Artifact Registry:** at most 2 images.
- **Neon:** **Billing / Usage**. Watch CU-hours (limit 100), storage and egress.
- **Vercel:** **Usage**. Watch Active CPU (4 h), requests and data transfer.

## 11. Tear down

When staging is no longer needed:

1. Google Cloud:

   ```bash
   gcloud run services delete brewdeck-api-staging
   gcloud artifacts repositories delete brewdeck --location="$REGION"
   gcloud secrets delete brewdeck-jwt-secret
   gcloud secrets delete brewdeck-db-password
   gcloud secrets delete brewdeck-mail-password
   gcloud projects delete "$GCP_PROJECT"
   ```

2. Neon: **Project settings → Delete project**.
3. Vercel: **Settings → Advanced → Delete Project**. Then remove the Vercel GitHub app
   (GitHub → Settings → Applications).
4. Gmail: revoke the app password at https://myaccount.google.com/apppasswords.

## Security notes

- **Open registration on a public URL:**
  - Anyone can create an account. Data stays per-user (ownership checks on every read and write).
  - Auth rate limits apply, but the per-IP limits act as one shared bucket behind the proxy
    ([ADR-011](../decisions/ADR-011-in-memory-auth-rate-limiting.md)). Per-account limits still work.
- **Direct API URL:** the `run.app` URL is public. It answers the same API with the same auth; the
  browser never needs it.
- **Rotating a secret:**

  ```bash
  printf '%s' "<new value>" | gcloud secrets versions add <secret> --data-file=-
  ```

  Then redeploy. Rotating `brewdeck-jwt-secret` logs everyone out.
- **Sensitive data:** do not put real personal data in staging.

## Next

Automating step 9 (a GitHub Actions workflow that deploys the API when `master` changes, using
Workload Identity Federation instead of keys) comes in a later PR.
