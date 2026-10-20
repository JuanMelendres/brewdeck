# Security checklist (ASVS-style)

Living checklist of BrewDeck's security controls, grouped by
[OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) area.
It closes backlog item AUTH-05 (audit of 2026-10-16). Each control links to the code that
implements it and the test that proves it. Re-check the list when a change touches auth,
ownership, tokens, configuration or headers. Add a row when you add a control.

- **Status:** `pass` = implemented and covered by a test; `gap` = known, tracked in
  [`docs/product/backlog.md`](../product/backlog.md).
- **Scope:** `brewdeck-api` and the security headers of `brewdeck-web`. ASVS requirement IDs are
  not cited on purpose; the areas follow the ASVS chapters.
- **Assumption:** the 2026-10-16 audit report itself was not stored in the repo. This list was
  rebuilt from the code and tests on 2026-10-16, after AUTH-05.1 to AUTH-05.8 were merged.

Paths are relative to `brewdeck-api/src/main/java/com/brewdeck/brewdeck_api/` (code) and
`brewdeck-api/src/test/java/com/brewdeck/brewdeck_api/` (tests), unless they start with
`brewdeck-web/`.

## Authentication

| Control | Status | Code | Test |
|---|---|---|---|
| Passwords hashed with Argon2id (m = 19 MiB, t = 2, p = 1), at most 4 hashes at once; legacy BCrypt hashes still match and are re-hashed on login; length 8 to 128 characters (ADR-018) | pass | `common/config/PasswordEncoderConfig`, `common/config/BoundedPasswordEncoder`, `auth/AuthService#login`, the auth request records | `common/config/PasswordEncoderConfigTest`, `auth/AuthServiceTest`, `integration/ValidationLimitsIntegrationTest` |
| New passwords (register, change, reset): at least 15 characters and not on the local common-password list (5.5k entries from SecLists, case-insensitive); login applies neither, so older short passwords still work (ADR-018) | pass | `common/validation/NotCommonPassword`, `common/validation/CommonPasswords`, the auth request records | `common/validation/CommonPasswordsTest`, `integration/ValidationLimitsIntegrationTest` |
| Password fields have a show/hide toggle | pass | `brewdeck-web/src/components/ui/PasswordField.tsx` | `brewdeck-web/src/components/ui/PasswordField.test.tsx` |
| Login failure is uniform: unknown email and wrong password give the same 401 body, and both run one BCrypt comparison (dummy hash) | pass | `auth/AuthService` | `auth/AuthServiceTest` (same exception for both); the dummy-hash comparison is code review only |
| Signup does not reveal registered emails: same 202 for new and existing addresses; the existing owner gets an "account exists" email (AUTH-05.7, ADR-011) | pass | `auth/AuthService`, `auth/verification/EmailVerificationService` | `integration/AuthSecurityIntegrationTest` |
| Forgot-password answers 200 for unknown emails and sends nothing | pass | `auth/reset/PasswordResetService` | `integration/PasswordResetIntegrationTest` |
| Per-IP and per-account rate limits on login, register, refresh, forgot/reset password, verify and resend; 429 with `Retry-After` (ADR-011) | pass | `common/ratelimit/RateLimitRule` | `integration/RateLimitIntegrationTest` |
| Per-IP limits see the real client IP behind the Next proxy | gap | `server.forward-headers-strategy: none` in `application.yaml` | PROD-01 (finding M1) |
| Email verification can be enforced: unverified users are blocked from writes, behind a flag (ADR-012) | pass | `auth/verification/EmailVerificationRequiredFilter` | `integration/EmailVerificationEnforcementIntegrationTest` |
| Reset and verification tokens: 32 random bytes, stored as SHA-256, single use, expire (30 min / 24 h) | pass | `common/security/SecureTokens`, `auth/reset/PasswordResetService`, `auth/verification/EmailVerificationService` | `integration/PasswordResetIntegrationTest`, `integration/EmailVerificationIntegrationTest` |
| Reset and verification tokens are consumed atomically, before any side effect (AUTH-05.5) | pass | `markUsedIfUnused` in `auth/reset/PasswordResetTokenRepository` and `auth/verification/EmailVerificationTokenRepository` | `auth/reset/PasswordResetServiceTest`, `auth/verification/EmailVerificationServiceTest`, repository tests |
| Email change needs the current password, a 1-hour single-use link from the new inbox, and notifies the old inbox with a cancel link; confirm and cancel end every session; a taken address gets the same 202 (AUTH-04, ADR-019) | pass | `auth/emailchange/EmailChangeService`, `EmailChangeRepository.confirmIfPending`/`cancelIfPending` | `auth/emailchange/EmailChangeServiceTest`, `integration/EmailChangeIntegrationTest` |
| Email change links act only on a click (mail scanners cannot trigger them) and send no referrer | pass | `brewdeck-web/src/components/auth/EmailChangeLinkView.tsx`, `src/lib/security/headers.ts` | `EmailChangeLinkView.test.tsx`, `headers.test.ts` |

## Session management

| Control | Status | Code | Test |
|---|---|---|---|
| Access JWT: HS256, 15 min TTL; expired, tampered, foreign-key, `alg: none` and malformed tokens are rejected (ADR-005) | pass | `auth/JwtService`, `auth/JwtAuthenticationFilter` | `integration/JwtEdgeCasesIntegrationTest` |
| Access tokens are revoked on logout, password change and reset (`ver` claim against `users.token_version`, AUTH-05.6) | pass | `auth/JwtAuthenticationFilter`, `auth/User#revokeAccessTokens` | `integration/AuthSecurityIntegrationTest` |
| Refresh token only in an httpOnly, Secure, `SameSite=Strict` cookie scoped to `/api/auth`; never in a response body; refresh requires `X-Requested-With` (ADR-013) | pass | `auth/refresh/RefreshTokenCookies` | `integration/AuthSecurityIntegrationTest` |
| Refresh rotation with reuse detection (a used token revokes every session); revoked tokens are only rejected | pass | `auth/refresh/RefreshTokenService` | `auth/refresh/RefreshTokenServiceTest`, `integration/AuthSecurityIntegrationTest` |
| Refresh rotation is atomic: of two concurrent rotations of one token, exactly one wins (AUTH-05.5) | pass | `RefreshTokenRepository#markUsedIfActive` | `integration/RefreshTokenRotationConcurrencyIntegrationTest` |
| Password change and reset revoke every refresh token; logout revokes the presented one | pass | `auth/refresh/RefreshTokenService` | `integration/AuthSecurityIntegrationTest`, `integration/PasswordResetIntegrationTest` |
| Server is stateless: no HTTP session; CSRF protection is off because the API authenticates with a Bearer header (the refresh cookie has its own header check above) | pass | `common/config/SecurityConfig` | `integration/AuthSecurityIntegrationTest` |

## Access control

| Control | Status | Code | Test |
|---|---|---|---|
| Every coffee, recipe and brew session read is scoped to the owner; foreign ids answer 404 (existence is not revealed) | pass | `findBy...AndOwnerId` finders in the repositories | `integration/CrossUserIsolationIntegrationTest` |
| Writes, deletes, favorite and share on foreign rows answer 404 and leave the row untouched | pass | services in `coffee/`, `recipe/`, `session/` | `integration/CrossUserWriteIsolationIntegrationTest` |
| Related ids in requests (a recipe's coffee and method, a session's recipe) must belong to the caller | pass | `recipe/RecipeService`, `session/BrewSessionService` | `integration/CrossUserWriteIsolationIntegrationTest`, `integration/BrewMethodOwnershipIntegrationTest` |
| Rankings, stats, dashboard and recipes-by-coffee/method only count the caller's rows (AUTH-05.8) | pass | `recipe/RecipeStatsService`, `coffee/CoffeeService`, dashboard service | `integration/CrossUserIsolationIntegrationTest` |
| The AI assistant only reads the caller's coffee, recipe and visible methods; nothing foreign reaches the prompt (AUTH-05.8) | pass | `ai/RecipeSuggestionService`, `ai/RecipeImprovementService` | `integration/CrossUserAiIsolationIntegrationTest` |
| Brew methods: shared catalog is read-only for users; private methods are visible and editable only by their owner (ADR-010) | pass | `method/BrewMethodService`, `method/AdminBrewMethodController` | `integration/BrewMethodOwnershipIntegrationTest` |
| `/api/admin/**` requires `ADMIN`; no endpoint grants roles (ADR-009) | pass | `common/config/SecurityConfig` | `integration/BrewMethodOwnershipIntegrationTest` |
| Admin bootstrap promotes an existing account only once its email is verified (AUTH-05.4) | pass | `auth/AdminBootstrap` | `auth/AdminBootstrapTest` |
| Share links: random token, read-only public view, unknown token is 404, unsharing kills the link | pass | `recipe/PublicRecipeController`, `recipe/RecipeService` | `integration/RecipeShareFlowIntegrationTest` |
| Disabled features are refused by the backend, not only hidden in the UI (ADR-007) | pass | `featureflag/FeatureFlagService#requireEnabled` | `integration/FeatureFlagIntegrationTest` |

## Validation, errors and logging

| Control | Status | Code | Test |
|---|---|---|---|
| Bean Validation on every request record; size limits match the columns | pass | `*Request` records | `integration/ValidationLimitsIntegrationTest` |
| Unknown sort properties give 400, not 500 | pass | `common/error/GlobalExceptionHandler` | `integration/InvalidSortIntegrationTest` |
| Errors use one `ErrorResponse` shape with HTML-escaped messages; unexpected exceptions return a generic 500 (stack trace only in the server log); request URIs cannot forge log lines | pass | `common/error/GlobalExceptionHandler` | `common/error/GlobalExceptionHandlerTest`, `integration/MvcErrorHandlingIntegrationTest` |
| Logs carry user ids, never passwords, tokens or (in mail delivery) recipient addresses | pass | `auth/mail/SmtpMailAdapter`, auth services | code review: no log statement takes an email or a raw token (checked 2026-10-16) |
| Staging and prod refuse to start with mail off, because the logging adapters would write live reset and verification links (AUTH-05.3) | pass | `common/config/StartupSafetyCheck` | `common/config/StartupSafetyCheckTest`, `integration/StagingProfileIntegrationTest` |

## Configuration and deployment

| Control | Status | Code | Test |
|---|---|---|---|
| The public dev JWT secret is refused outside the `local` and `test` profiles; staging and prod have no secret default (AUTH-05.2) | pass | `common/config/StartupSafetyCheck`, `application-staging.yml`, `application-prod.yml` | `common/config/StartupSafetyCheckTest` |
| Secrets come from the environment (Secret Manager on staging); `.env` is not committed | pass | `application*.yml`, `.env.example` | manual: live staging revision checked 2026-10-16 |
| OpenAPI and Swagger UI are off in staging and prod | pass | `application-staging.yml`, `application-prod.yml` | `integration/ProdProfileApiDocsIntegrationTest`, `integration/StagingProfileIntegrationTest` |
| CORS: exact allowed origins, only `Authorization` and `Content-Type` request headers, no credentials | pass | `common/config/WebConfig` | `integration/CorsIntegrationTest` |
| Actuator exposes only `health` and `info`; health details hidden | pass | `application.yaml` (`show-details: never`) | `integration/HealthEndpointIntegrationTest` (health is public and UP); exposure list is code review only |
| API image runs as a non-root user | pass | `brewdeck-api/Dockerfile` | manual: image check in the free hosting spike |
| Cloud Run `run.app` URL is only reachable through the Next proxy | gap | none yet | PROD-01 (deferred to production) |
| Dependency CVEs: OWASP Dependency-Check weekly and in the `security` profile (`failBuildOnCVSS=7`); SonarCloud on every PR | pass | `pom.xml`, `.github/workflows/security.yml` | CI |

## Web security headers (`brewdeck-web`)

| Control | Status | Code | Test |
|---|---|---|---|
| `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy` (`no-referrer` on share, reset and verify pages), `Permissions-Policy` (AUTH-05.1) | pass | `brewdeck-web/src/lib/security/headers.ts` | `brewdeck-web/src/lib/security/headers.test.ts` |
| Content Security Policy is enforced | gap | sent as Report-Only, with `'unsafe-inline'` | AUTH-05.1 follow-up: enforce after the staging console check, then a per-request nonce |

## Open gaps

| Gap | Backlog |
|---|---|
| Per-IP rate limits act as one global bucket behind the Next proxy | PROD-01 (M1) |
| `run.app` URL bypasses Vercel's firewall | PROD-01 |
| CSP is Report-Only and allows `'unsafe-inline'` | AUTH-05.1 follow-up |
| AUTH-01 (reported login redirect) not reproducible from code | AUTH-01: needs a repro on staging |
