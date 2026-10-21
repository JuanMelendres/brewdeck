# ADR-022: End-to-end tests with Playwright

## Status
Accepted (2026-10-21). Backlog QA-01.

## Date
2026-10-21

## Context
- Coverage today is unit, component and integration tests:
  - API: JUnit, Mockito, MockMvc, Testcontainers;
  - web: Vitest and React Testing Library with mocked API hooks.
- Nothing checks that the real web, the Next proxy, the API, Postgres and email work together.
  Those paths are checked by hand (`docs/testing/manual-test-checklist.md`). Breakages there are
  found late:
  - the refresh cookie through the proxy (ADR-013);
  - the verification link in a real email;
  - the order coffee → recipe → brew session.
- Mailpit already catches local email (`docker-compose.yml`) and has an HTTP API to read messages.
- The repository is public, so GitHub Actions minutes are free.

## Decision

**Tool: Playwright (`@playwright/test`).**
- It is a dev dependency of `brewdeck-web`, installed with pnpm. Tests live in `brewdeck-web/e2e/`
  with their own `playwright.config.ts`. Vitest ignores that folder.
- Two projects: Chromium at desktop size, and Chromium at phone size (Pixel 7). There is no
  Firefox or WebKit for now; the DATA-01 browser checks stay manual.
- The browser locale is `es-MX`, so the app picks Spanish from `Accept-Language`. Tests use
  accessible locators (`getByRole`, `getByLabel`) with the text from `messages/es.json`. No test
  ids, as in the component tests.

**First suite: one smoke journey.**
1. Sign up with a unique address (`e2e-<timestamp>@example.com`).
2. Read the verification email from the Mailpit API and open the link.
3. Log in.
4. Create a coffee, a recipe for it, and a brew session for the recipe.
5. Reload the page and stay signed in (refresh cookie through the proxy).
6. Log out, and check that a protected page sends you to login.

**Stack under test.**
- **CI:** a new workflow `e2e.yml`.
  - Postgres 16 and Mailpit run as service containers.
  - The API runs from its jar with the `local` profile. Mail goes to Mailpit, AI is off, and the
    rate limits stay on.
  - The web runs as a production build (`pnpm build`, `pnpm start`), proxying `/api` to the API.
  - The database is new on every run.
- **Local:** `docker compose up -d postgres mailpit`, then the API with the mail variables, then
  `pnpm e2e`. Playwright starts the web if it is not running. Test accounts land in the local
  database under `e2e-…@example.com`.

**When it runs.**
- On pull requests into `develop` and `master` that touch `brewdeck-api/**`, `brewdeck-web/**`
  or the workflow, and by hand (`workflow_dispatch`).
- It is not a required check at first. After two weeks without flaky runs, add it to the branch
  protection with `api-ci` and `web-ci` (OPS-04).
- On failure, the HTML report and the traces are uploaded as artifacts for 7 days.

**Out of scope for now (backlog QA-01 follow-ups):**
- automated accessibility checks (`@axe-core/playwright`);
- a post-deploy smoke test against staging, which needs a real inbox to sign up;
- more journeys: password reset, email change, export and delete.

## Consequences
- **Positive:**
  - The main journey is checked on every change, through the real proxy, cookie and email.
  - Manual checks that become automated leave the manual checklist.
- **Negative:**
  - CI takes about 6 to 8 more minutes per PR: the API build, the web build and the browser
    install.
  - E2E tests are slower and can be flaky. Mitigations:
    - web-first assertions with no fixed waits;
    - one retry in CI;
    - a trace for every failure.
  - Spanish UI text in locators breaks when copy changes. That is acceptable: the copy is part of
    the journey.

## Alternatives considered
- **Cypress.** Mature, but slower in CI, weaker on multiple tabs and origins, and it would add a
  second test runner style. Rejected.
- **Only more component tests.** They cannot catch proxy, cookie or email faults. Rejected.
- **Running E2E against staging.** It uses shared data and a real inbox, and staging is only
  updated on release. Rejected as the main suite; kept as a later smoke test.
- **Docker Compose for the whole stack in CI.** It needs an API image build on every run.
  Service containers plus the jar are faster and simpler. Rejected for now.
