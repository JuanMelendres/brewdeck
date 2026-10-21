# End-to-end testing (Playwright)

The E2E suite drives a real browser through the web app, the Next proxy, the API, Postgres and
email. Design and scope: [ADR-022](../decisions/ADR-022-e2e-tests-playwright.md) (backlog QA-01).

| What | Where |
|---|---|
| Specs | `brewdeck-web/e2e/*.spec.ts` |
| Mailpit helper (reads emails) | `brewdeck-web/e2e/mailpit.ts` |
| Config | `brewdeck-web/playwright.config.ts` |
| CI | `.github/workflows/e2e.yml` |

## The suite

`smoke.spec.ts` runs one journey, at desktop size and at phone size (Pixel 7):

1. Sign up with a unique `e2e-<timestamp>-<project>@example.com`.
2. Open the verification link from the email in Mailpit.
3. Log in and close the first-login dialog.
4. Add a coffee, a recipe for it, and a brew session for the recipe.
5. Reload the page and stay signed in. This checks the refresh cookie through the proxy.
6. Log out. `/coffees` then sends you to login.

**Locators:**
- The browser locale is `es-MX`, so the UI is Spanish.
- Use accessible locators with the text from `messages/es.json`: `getByRole` with a name, or
  `getByLabel`.
- When one label is part of another, pass `{ exact: true }` or use the role. For example,
  "Contraseña" is part of "Confirma la contraseña", and "Café" is part of "Gramos de café".
- No fixed waits. Use web-first assertions (`await expect(...).toBeVisible()`) and
  `expect.poll` for email.

## Run it locally

You need Postgres, Mailpit, the API with mail sent to Mailpit, and a production build of the web.

```bash
docker compose up -d postgres mailpit

# Terminal 1: the API, sending mail to Mailpit
cd brewdeck-api
BREWDECK_MAIL_ENABLED=true SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=1025 \
  AI_ENABLED=false ./mvnw spring-boot:run

# Terminal 2: build the web once, then run the suite (Playwright starts `pnpm start`)
cd brewdeck-web
pnpm exec playwright install chromium   # first time only
pnpm build
pnpm e2e                                # or: pnpm e2e --project=desktop --headed
```

- **Reports:** `pnpm exec playwright show-report` opens the last HTML report. A failed test keeps
  a trace: `pnpm exec playwright show-trace test-results/<test>/trace.zip`.
- **Accounts:** each run adds `e2e-…@example.com` accounts to the local database. They are test
  data and safe to delete.
- **Rate limits:** sign-up allows 5 per IP per hour (`REGISTER_IP`), and each run signs up twice.
  After a few runs, sign-up answers "Demasiados intentos". Restart the API: the limits live in
  memory.
- **Different ports:** set `E2E_BASE_URL` (web), `MAILPIT_URL` (Mailpit UI and API, default
  `http://localhost:8025`), and `API_PROXY_TARGET` (the API address, for `pnpm build` and `pnpm start`). The API must allow the web
  origin: `CORS_ALLOWED_ORIGINS` and `BREWDECK_MAIL_FRONTEND_BASE_URL` set to the web URL. If the
  origin is not allowed, sign-up only shows "No pudimos crear la cuenta".

## CI

`e2e.yml` runs on pull requests into `develop` and `master` that touch the API, the web or the
workflow. It also runs by hand under **Actions → BrewDeck E2E → Run workflow**.

- **Stack:** Postgres 16 and Mailpit as service containers, the API jar with the `local` profile,
  and the web production build. The workflow starts the web in its own step, not through
  Playwright's `webServer`. When Playwright owned `pnpm start`, the run hung at the end, waiting
  for `next-server` to exit.
- **Time limit:** the whole run stops after 5 minutes (`globalTimeout`).
- **Failures:** one retry. The HTML report, the traces and the API log are uploaded for 7 days.
- **Not required yet.** After two weeks without flaky runs, add `Playwright smoke` to the branch
  protection (OPS-04).

## Adding a journey

- Sign up a fresh user per test. Sharing users across tests makes them depend on each other.
- Mind the sign-up limit: CI signs up once per test per project, plus retries, within one API
  lifetime. With more than two sign-up tests, add a fixture that signs up once per worker.
- Move manual checks that become automated out of
  [manual-test-checklist.md](manual-test-checklist.md).
