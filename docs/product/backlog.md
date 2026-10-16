# BrewDeck Backlog

The single source of truth for pending work: bugs, features, hardening and operations. Roadmap
phases live in [roadmap.md](roadmap.md); finished work lives in `.claude/project-state.md`.

**Last triage:** 2026-10-16. Owner reports were checked against `develop`. "Evidence" points at
the code, not at the report.

**How to use it:**

- Pick work top-down within a sprint, one PR per item (or per sub-item for L).
- Update **Status** in the same PR that changes it.
- Mark items done with ~~strikethrough~~ and the PR number. Prune done items at the next triage.
- New reports go to "Inbox" until they are triaged.

| Status | Meaning |
|---|---|
| `implemented` | Done; at most polish left |
| `partial` | Some of it exists; the gap is described |
| `missing` | Not built |
| `needs-repro` | Reported, but the code does not show it; reproduce first |
| `owner` | Needs Juan (decision, account, or a dashboard toggle) |

**Priority:** **P0** before real users (production go-live), **P1** next product iteration,
**P2** progressive improvement.

**Size:** S < half a day, M about a day, L multiple PRs.

## Sprint A — before real users (P0)

| ID | Item | Status | Evidence (2026-10-16) | Next step | Size |
|---|---|---|---|---|---|
| ~~AUTH-01~~ | ~~Sign Up asks for the password only once~~ (#253) | `implemented` | Owner clarified on 2026-10-16: the Login and Sign Up screens route correctly (no redirect bug); the real gap was the missing confirmation field. `RegisterForm` now asks for the password twice; `registerSchema` rejects a mismatch on `confirmPassword`, and only email and password reach the API. | — | S |
| AUTH-02 | Email verification before using the account | `partial` | Tokens: random, SHA-256 hashed, 24 h TTL, single use; resend limited to 3 per hour (`RESEND_VERIFICATION_EMAIL`). There is a blocking filter (`EmailVerificationRequiredFilter`, 403 `EMAIL_NOT_VERIFIED`), a frontend gate and ADR-012. SMTP works on staging since #248. Enforced in `staging` by V29 (#254). **Gap:** the flag is still off in `prod`; there is no `PENDING_VERIFICATION` status (a boolean only); no bounce monitoring. | After the next release, sign up a fresh account on staging and confirm it is blocked until verified. Then decide whether a status column is needed (AUTH-06 depends on it). | S–M |
| AUTH-03 | Password policy per NIST SP 800-63B | `partial` | Minimum is 8 (`RegisterRequest`, `ChangePasswordRequest`); maximum 72 **bytes** (BCrypt limit, V17 / audit item 4); no composition rules, no forced rotation, revocation on change and reset exists. **Gaps:** minimum 15 for a single factor, a blocklist of common or breached passwords, at least 64 characters (72 bytes cuts multibyte passphrases short), show/hide toggle. | ADR: Argon2id (no 72-byte limit) with gradual rehash, vs. BCrypt plus pre-hashing; local blocklist (no plaintext to third parties); rules in backend and Zod. | M |
| AUTH-04 | Secure email change | `missing` | `UpdateProfileRequest` has no email field, so email cannot be changed at all; nothing is exposed. | Design with re-authentication, `pending_email`, a token to the new address, a notice to the old one, and session revocation. ADR first. | L |
| ~~AUTH-05~~ | ~~Authorization, ownership and session audit~~ | `implemented` | Audit run 2026-10-16 (`security-auditor`, read-only): no Critical or High findings. AUTH-05.1 to AUTH-05.8 fixed (#255, #256, #257, #258, #259, #260, #262). Checklist (#263): [`docs/security/asvs-checklist.md`](../security/asvs-checklist.md). | Open gaps live in PROD-01 and the AUTH-05.1 CSP follow-up. | M |
| ~~AUTH-05.1~~ | ~~Security headers and CSP on the Next app (M2)~~ (#255) | `implemented` | `src/lib/security/headers.ts` adds `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy` (`no-referrer` on share, reset and verify pages), `Permissions-Policy` and a CSP in **Report-Only**. | Check the browser console on staging; then switch to the enforced header and, later, a per-request nonce instead of `'unsafe-inline'`. | S |
| ~~AUTH-05.2~~ | ~~Fail startup on the dev JWT secret outside `local`/`test` (M3)~~ (#258) | `implemented` | `application.yaml` falls back to `dev-only-insecure-secret...` whenever `BREWDECK_JWT_SECRET` is unset; an image run without the staging/prod profile accepts forged tokens. | `StartupSafetyCheck` refuses the dev secret unless the `local` or `test` profile is active. Live staging checked 2026-10-16 (revision `00006`): `SPRING_PROFILES_ACTIVE=staging`, `BREWDECK_JWT_SECRET` from Secret Manager, mail on. | S |
| ~~AUTH-05.3~~ | ~~Fail startup in staging/prod when mail is off (L5)~~ (#258) | `implemented` | With `brewdeck.mail.enabled=false`, the logging mail adapters write full reset and verification links to the logs. | `StartupSafetyCheck` refuses `brewdeck.mail.enabled=false` under `staging` or `prod` (same PR as AUTH-05.2). | S |
| ~~AUTH-05.4~~ | ~~Admin bootstrap only promotes verified accounts (L4)~~ (#259) | `implemented` | `AdminBootstrap` grants ADMIN to `BREWDECK_ADMIN_EMAIL` without checking `emailVerified`, on every start. | `AdminBootstrap` skips accounts whose email is not verified and logs a warning; existing admins are not demoted. Docs: ADR-009, environment-variables.md, staging-deployment.md step 8. | S |
| ~~AUTH-05.5~~ | ~~Atomic refresh-token rotation (L2)~~ (#260) | `implemented` | Conditional claim `markUsedIfActive` / `markUsedIfUnused` (`UPDATE ... WHERE used_at IS NULL`) in the refresh, reset and verification repositories; 0 rows = reuse (refresh) or used link (reset, verification), checked before any side effect. `RefreshTokenRotationConcurrencyIntegrationTest` fires 4 concurrent rotations of one token (fails 5/5 on the old code). | ADR-013 updated. | S–M |
| ~~AUTH-05.6~~ | ~~Revoke access tokens on logout and password change (L1)~~ (#256) | `implemented` | Decided 2026-10-16: fix. `users.token_version` (V30) travels in the JWT as `ver`; logout, password change and reset bump it. Password change returns a fresh session for the caller. A revoked refresh token is now rejected without triggering reuse detection. ADR-013 addendum. | — | M |
| ~~AUTH-05.7~~ | ~~Stop signup from revealing registered emails (L3)~~ (#257) | `implemented` | Option A (owner, 2026-10-16). `register` answers `202` with the same message for new and existing emails and creates no session; a new address gets the verification email, an existing one an "account exists" email (EN/ES). `REGISTER_EMAIL` limit (3/hour) before the lookup; one BCrypt hash on both paths. The web shows "Check your email" and the user logs in afterwards. | — | M |
| ~~AUTH-05.8~~ | ~~Cross-user tests for the remaining endpoints~~ (#262) | `implemented` | `CrossUserIsolationIntegrationTest` adds `/recipes/coffee/{id}`, `/recipes/method/{id}` (empty page for foreign data), `/coffees/most-used` and `/recipes/most-brewed` (foreign rows seeded with 25 recipes/sessions, so a leak would rank first). New `CrossUserAiIsolationIntegrationTest`: flag on, model mocked; `/recipes/{id}/improve` on a foreign recipe and `/recipes/suggest` with a foreign coffee or private method answer 404 with the resource message and never reach the model; own-data controls answer 200. | Write the ASVS-style checklist (AUTH-05). | S |
| LEGAL-01 | Privacy notice, terms, data export and deletion | `missing` + `owner` | No pages, no consent, no account deletion or export endpoint. | Owner decides the operator identity and contact. Draft the privacy notice (LFPDPPP, ARCO rights) and the T&C; get a professional review before publishing. Build: footer links, consent at signup with version and date, account deletion and export. | L |
| PROD-01 | Restrict the `run.app` URL to the Next proxy | `missing` | Cloud Run is public and bypasses Vercel's firewall. Deferred to production (decided 2026-10-16). Also fixes audit finding M1: with `forward-headers-strategy: none` the per-IP auth rate limits see the proxy address, so they act as one global bucket; once the shared secret exists, trust the client-IP header the Next proxy sets. | Shared-secret header set by Next and checked by an API filter; secret in Secret Manager and Vercel. | M |

## Sprint B — stability and domain (P1)

| ID | Item | Status | Evidence | Next step | Size |
|---|---|---|---|---|---|
| AUTH-06 | Retention and purge of unverified accounts | `missing` | No scheduled job; no `verified_at` / status columns beyond the boolean. Depends on AUTH-02 being enforced. | ADR for the policy (proposal: purge unverified accounts with no activity after 7 days). Idempotent batched job, tested with a fake clock; never touch verified accounts. | M |
| DATA-01 | Numeric precision and presentation | `partial` | Audit done 2026-10-16 (#266): [`docs/architecture/numeric-fields.md`](../architecture/numeric-fields.md). Probe confirmed G1 (`waterTemp: 92.7` stored as 92 with 201) and G2 (`coffeeGrams: 15.126` answered as sent, stored as 15.13). Also found: unit messages and averages are not locale-formatted (G3, G4); `ratio` free text (G5). Fix 1 (decimal temperatures, V31) on branch `feat/api-decimal-temperatures`. | Fix PRs in the doc's order: temperatures to `NUMERIC(4,1)` (owner, 2026-10-16: both temps keep one decimal), grams scale, locale formatting, browser check, ratio option A (derive from grams, decided 2026-10-16). | M |
| ~~UX-02~~ | ~~Coffee → Recipe → Brew Session dependency guidance~~ (#264) | `implemented` | Recipes page with no coffee: Add Recipe disabled, the empty state asks for a coffee and opens the coffee form; saving it opens the recipe form with that coffee selected. Brew sessions page with no recipe: same, via the recipe form into the brew form with the new recipe; with no coffee either, it walks coffee → recipe → brew session. `CoffeeFormDialog` and `RecipeFormDialog` gained `onCreated`; `RecipeFormDialog` gained `initialCoffeeId`. | Check the flow in the browser on staging. | S–M |
| ~~BREW-01~~ | ~~Brew session detail, edit and delete; cards on mobile~~ (#265) | `implemented` | New `/brew-sessions/[id]` (`BrewSessionDetailView`): every field, recipe link, Edit (`BrewSessionFormDialog` in edit mode, `PUT`) and Delete with confirmation (`DeleteBrewSessionDialog`), then back to the list. The brew date in the table (also on the recipe page's history) links to the detail. Under `sm` the list renders `BrewSessionCards` instead of the table. | Check on a phone on staging. | M–L |
| OPS-01 | Ollama manual check after the Jackson 3 port | `owner` | #239 left one manual AI suggestion against `ollama serve` (local) unchecked. | Run one suggest and one improve locally. | S |
| OPS-02 | Review the round-3 CVE suppressions | `partial` | 11 spring-core + 2 spring-security-core suppressions (2026-09-22); Boot 4 changed those versions. | `security-auditor` review; remove the stale ones. | S |

## Sprint C — adoption and quality (P1/P2)

| ID | Item | Prio | Status | Evidence | Next step | Size |
|---|---|---|---|---|---|---|
| QA-01 | End-to-end QA strategy | P1 | `partial` | Strong unit, component and integration coverage (JUnit, Mockito, MockMvc, Testcontainers, Vitest/RTL; about 600 + 427 tests); `docs/testing/` has a strategy and a plan. **Gaps:** no Playwright E2E; no a11y automation; no post-deploy smoke test. | Playwright smoke: signup → verify (Mailpit) → login → coffee → recipe → session → logout; run in CI against Compose; later after staging deploys. | L |
| UX-01 | First-use onboarding | P1 | `partial` | The first-login dialog asks for language and theme (`ThemeOnboardingDialog`); no domain onboarding. | 3-step progress (coffee → recipe → session) shown on the dashboard empty state, stored per user, skippable. | M |
| UX-03 | Contextual help and tours | P2 | `missing` | Nothing. | After UX-01; field help for units, an example recipe, a help button. | M |
| QA-02 | BrewDeck QA skill for Claude | P2 | `missing` | `.claude/skills/` has `testing-quality` but no QA runner. The external candidate (`adampaulwalker/qa-test`) is unreviewed. | Review the external skill (license, maintenance, dependencies) or write `.claude/skills/brewdeck-qa` with discover/smoke/auth/api/full modes and reports in `docs/testing/reports/`. | M |

## Operations and platform

| ID | Item | Prio | Status | Next step |
|---|---|---|---|---|
| OPS-03 | Vercel firewall toggles | P1 | `owner` | Project → Firewall: AI Bots → Deny; Bot Protection → Log, then Challenge. |
| OPS-04 | Branch protection on `master` requiring `api-ci` and `web-ci` | P1 | `owner` | The deploy pipeline trusts that release PRs passed CI. |
| OPS-05 | GCP trial ends 90 days after 2026-10-15 | P0 (date) | `owner` | Calendar: around **2027-01-03** (day 80), Billing → Activate full account, or staging stops. |
| OPS-06 | `brew-recipe-ai-assistant` flag expires 2026-12-01 | P1 | `owner` | Extend the expiry, or decide removal (AI is local only). |
| OPS-07 | `auth-require-email-verification` flag expires 2026-12-31 | P1 | linked | Covered by AUTH-02 rollout. |
| OPS-08 | OWASP scan duration | P2 | watch | With the NVD key, a cold cache took about 50 min. Check that the weekly run on `develop` warms a shared cache; if not, rework the cache key. |
| OPS-09 | Web deploy can land before the API | P2 | known | Vercel and the API pipeline run in parallel. Acceptable for staging; for production, make the web wait for the API (or deploy the web from Actions). |
| PROD-02 | Production hosting decision (home lab vs cloud), custom domain, Cloudflare in front | P1 | `owner` | Spike when go-live is near; Cloudflare needs a custom domain. |
| PROD-03 | Production email provider and domain (SPF/DKIM) | P1 | `owner` | A Gmail app password is fine for staging; production needs a domain-based sender. |
| PROD-04 | Backups and restore drill for the production database | P1 | `missing` | Neon Free has 6 h point-in-time restore only; define the production plan. |

## Inbox (untriaged)

Empty.
