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
| AUTH-01 | Unknown email at login sends the user to Sign Up | `needs-repro` | Backend answers the same 401 for an unknown email and for a wrong password, with a dummy BCrypt check for equal timing (`AuthService.login`). `LoginForm` shows `t('failed')` and only pushes `/dashboard` on success. `apiFetch` sends 401s to `/login`, never to `/register`. | Reproduce on staging (browser, steps, URL before and after); the password manager or browser autofill is a suspect. Separately, decide on signup enumeration: `register` returns 409 "Email is already registered". | S |
| AUTH-02 | Email verification before using the account | `partial` | Tokens: random, SHA-256 hashed, 24 h TTL, single use; resend limited to 3 per hour (`RESEND_VERIFICATION_EMAIL`). There is a blocking filter (`EmailVerificationRequiredFilter`, 403 `EMAIL_NOT_VERIFIED`), a frontend gate and ADR-012. SMTP works on staging since #248. Enforced in `staging` by V29 (#PR). **Gap:** the flag is still off in `prod`; there is no `PENDING_VERIFICATION` status (a boolean only); no bounce monitoring. | After the next release, sign up a fresh account on staging and confirm it is blocked until verified. Then decide whether a status column is needed (AUTH-06 depends on it). | S–M |
| AUTH-03 | Password policy per NIST SP 800-63B | `partial` | Minimum is 8 (`RegisterRequest`, `ChangePasswordRequest`); maximum 72 **bytes** (BCrypt limit, V17 / audit item 4); no composition rules, no forced rotation, revocation on change and reset exists. **Gaps:** minimum 15 for a single factor, a blocklist of common or breached passwords, at least 64 characters (72 bytes cuts multibyte passphrases short), show/hide toggle. | ADR: Argon2id (no 72-byte limit) with gradual rehash, vs. BCrypt plus pre-hashing; local blocklist (no plaintext to third parties); rules in backend and Zod. | M |
| AUTH-04 | Secure email change | `missing` | `UpdateProfileRequest` has no email field, so email cannot be changed at all; nothing is exposed. | Design with re-authentication, `pending_email`, a token to the new address, a notice to the old one, and session revocation. ADR first. | L |
| AUTH-05 | Authorization, ownership and session audit | `partial` | Phase 6 Slice B: owner-scoped reads and writes and `findByIdAndOwnerId`; cross-user isolation tests for reads and writes (`CrossUser*IntegrationTest`, `ResourceOwnershipIntegrationTest`); JWT edge cases; httpOnly `SameSite=Strict` refresh cookie (ADR-013); revocation on password change; rate limits (ADR-011); `/api/**` needs a JWT. **Gaps:** no explicit security headers / CSP on the Next side (Spring Security defaults only); share links not re-audited since Phase 6; no written ASVS-style checklist. | Run the `security-auditor` agent over auth, ownership, share links and headers; fix findings one PR each. | M |
| LEGAL-01 | Privacy notice, terms, data export and deletion | `missing` + `owner` | No pages, no consent, no account deletion or export endpoint. | Owner decides the operator identity and contact. Draft the privacy notice (LFPDPPP, ARCO rights) and the T&C; get a professional review before publishing. Build: footer links, consent at signup with version and date, account deletion and export. | L |
| PROD-01 | Restrict the `run.app` URL to the Next proxy | `missing` | Cloud Run is public and bypasses Vercel's firewall. Deferred to production (decided 2026-10-16). | Shared-secret header set by Next and checked by an API filter; secret in Secret Manager and Vercel. | M |

## Sprint B — stability and domain (P1)

| ID | Item | Status | Evidence | Next step | Size |
|---|---|---|---|---|---|
| AUTH-06 | Retention and purge of unverified accounts | `missing` | No scheduled job; no `verified_at` / status columns beyond the boolean. Depends on AUTH-02 being enforced. | ADR for the policy (proposal: purge unverified accounts with no activity after 7 days). Idempotent batched job, tested with a fake clock; never touch verified accounts. | M |
| DATA-01 | Numeric precision and presentation | `partial` | Grams are `DECIMAL(6,2)` + `BigDecimal` (correct). Temperatures are `INTEGER`. Rating is `INTEGER` 1–10 (sessions). Averages come back as `Double` from JPQL. **Gaps to audit:** `ratio` is `VARCHAR(20)` (free text, not computed); average rounding and display rules; NaN / null vs 0 in charts; locale decimal separator in forms. | Audit doc per field (unit, range, scale, display), then small fixes with tests. | M |
| UX-02 | Coffee → Recipe → Brew Session dependency guidance | `partial` | Empty states exist (`EmptyState`, e.g. "No brew sessions yet"); the backend validates ownership of related IDs. No "create a coffee first" guard on recipe creation, nor "create a recipe first" on session creation. | Disabled action plus a call to action to the missing dependency; return to the form afterwards. | S–M |
| BREW-01 | Brew session detail, edit and delete; cards on mobile | `missing` | There is only `brew-sessions/page.tsx` (a table; taste notes expand since #249). No `[id]` route, and no edit or delete in the web, although the API has full CRUD. | `/brew-sessions/[id]` with all fields and Edit/Delete (with confirmation); responsive cards under `sm`. | M–L |
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
