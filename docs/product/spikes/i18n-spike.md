# Technical Spike: Internationalization (Spanish + English)

## 1. Summary

BrewDeck is English-only. Workstream 8 of the Phase 7 UI/UX refresh adds Spanish, with a per-user language
preference. This spike answers how to translate the web UI, format dates and numbers per language, translate
backend messages and emails, and store the preference, without rewriting routing or the auth flow.

- **Main question:** which i18n approach fits a client-rendered, authenticated Next 16 app, and how does the
  language reach the backend?
- **Expected decision:** library, locale source, storage, backend approach, and the PR plan.

Origin: [UI/UX refresh spike](ui-ux-refresh-spike.md) §17 row 8 (decided 2026-10-01: Spanish + English, per-user
preference in Account, its own spike first).

## 2. Background

Repository facts (2026-10-05):

- **Frontend:** 83 non-test `.tsx` files under `src/components` and `src/app`, with roughly 250 unique English UI strings. That count is a rough grep (about 150 quoted literals + about 90 JSX text lines), so treat it as an order of magnitude.
- **Zod schemas:** about 60 English messages in `src/lib/validation/*Schema.ts` (Zod `^4.6.5`).
- **Dates:**
  - Every UI date goes through `UI_LOCALE = 'en-US'` in `src/lib/format/dates.ts` (#184), and `DashboardView` uses it for the greeting date.
  - The recipe PDF (`src/lib/pdf/recipePdf.ts`) uses `formatDate`.
  - Nothing else calls `Intl` or `toLocale*`.
- **Rendering:** components are `'use client'`. The session lives in memory and is restored on load through the refresh cookie (ADR-013). The root layout hardcodes `<html lang="en">`.
- **Theme preference precedent:** workstream 7 stores `users.theme_preference` (V22), returns it in `/me`, writes it with `PUT /api/auth/me/theme`, and caches it in the browser. The first-login dialog asks for it once.
- **Backend:**
  - About 54 Bean Validation `message = "..."` strings on request records, in English.
  - No `MessageSource`, `LocaleResolver`, or `Accept-Language` handling anywhere in `src/main/java`.
  - `SmtpMailAdapter` builds the verification and password-reset emails from English literals (subjects "Verify your BrewDeck email" and "Reset your BrewDeck password").
  - The latest migration is V22.
- **User data stays as entered:** coffee names, roast level (free text), notes, and brew-method names are not translated.
- **Next 16.3.6 guide** (`node_modules/next/dist/docs/01-app/02-guides/internationalization.md`): it shows locale routing with a `[lang]` segment plus `proxy.js`, and dictionaries for content. It does not require routing for localization.
- **`next-intl`:** 4.14.9 (npm, checked 2026-10-05) declares peer support for `next ^16` and `react ^19`.

## 3. Problem Statement

Every user-facing string is an English literal, in the components, the Zod schemas, the backend validation
messages, and the emails. We need Spanish and English chosen per user, with dates and numbers in the same
language, and no mixed-language screens.

## 4. Goals

- Pick the translation library and the message-file layout.
- Decide where the active locale comes from (before login, after login, on the server render).
- Decide how the backend learns the language (validation messages, errors, emails).
- Define the language preference storage (mirroring the theme preference).
- Split the work into shippable PRs, with a feature flag where needed.

## 5. Non-Goals

- No locale in URLs (`/es/...`) and no SEO work: the app is behind a login. The public share page is the only anonymous page (§21, OQ-003).
- No translation of user-entered data or the brew-method catalog.
- No languages beyond Spanish and English. No regional variants (one `es`, one `en`).
- No right-to-left support.
- No translation management service (Crowdin, i18nexus): the owner translates the Spanish file.

## 6. Key Questions

- Q-001: Library: `next-intl`, `react-i18next`, or an in-house dictionary?
- Q-002: Locale routing (`[lang]` segment) or a locale held outside the URL?
- Q-003: Locale source before login, after login, and during the server render (to avoid an English flash)?
- Q-004: How do backend validation messages and errors get translated?
- Q-005: How do emails pick a language, given they are sent asynchronously after commit?
- Q-006: How do Zod messages get translated?
- Q-007: Is a feature flag needed, and for what?

## 7. Constraints

- Keep the App Router structure, the auth flow (ADR-013), and the theme approach (ADR-014).
- Strict TypeScript: translation keys should be type-checked.
- Behavioral tests query by visible text: tests must keep running in English by default.
- Backend validation messages must avoid special symbols (CLAUDE.md: responses are sanitized).
- Feature Flag Policy: a multi-PR feature that should not be exposed half-done needs a flag, with the backend as
  the source of truth.

## 8. Options Considered

### Library (Q-001)

| Option | Description | Pros | Cons | Complexity | Risk |
| --- | --- | --- | --- | --- | --- |
| A · `next-intl` without routing | `NextIntlClientProvider` + `useTranslations`; ICU messages; `useFormatter` for dates/numbers | Built for the App Router; ICU plurals; typed keys from the English file; works in client and server components; formatters replace `UI_LOCALE` | One more dependency; its server config reads the locale per request (see Risk R-002) | Medium | Low |
| B · `react-i18next` | i18next with a React provider | Very mature; huge ecosystem | Client-first, extra wiring for the server render; plural syntax is not ICU; typed keys need more setup | Medium | Low |
| C · In-house dictionary | JSON per language + a small `t()` (the Next guide's pattern) | No dependency | No plurals or interpolation rules; date/number formatting stays hand-made; typed keys hand-built | Low at first, grows | Medium |

### Locale routing (Q-002)

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| `[lang]` segment + `proxy.js` redirect | Shareable localized URLs, SEO | Moves every route under `app/[lang]`; auth redirects, links, and tests change; no SEO need behind a login | Rejected |
| Locale outside the URL (cookie + user preference) | No route changes | URLs do not carry the language | **Chosen** |

## 9. Evaluation Criteria

| Criterion | Description | Weight |
| --- | --- | --- |
| Fit with the App Router | Works in client components and the server render | High |
| Type safety | Missing or misspelled keys fail `pnpm type-check` | High |
| Formatting | Plurals, dates, and numbers per locale | High |
| Change size | Touches strings, not routes or auth | High |
| Dependency cost | Bundle size, maintenance | Medium |

## 10. Research Findings

### Finding 1: Routing is not needed

Everything except `/share/[token]` and the auth pages sits behind a login. Users pick their language in Account, so the URL does not need to carry it. The Next 16 guide treats routing and localization as separate steps (§2).

Impact: no `app/[lang]` restructure. The locale lives in a cookie plus the user preference.

### Finding 2: The server render needs the locale, so a cookie is required

Pages are client components, but Next still renders them on the server. If the locale lived only in
`localStorage` (as the theme does), the first paint would be English and then switch: a visible flash. The theme
avoids this with a pre-hydration class switch (`InitColorSchemeScript`); text cannot be switched that way.

Impact: a `brewdeck-locale` cookie, readable by the root layout, sets `<html lang>` and the messages for the server
render. The `/me` preference is the source of truth and rewrites the cookie when it differs.

### Finding 3: The backend has no locale handling yet

There is no `MessageSource` and no `LocaleResolver` (§2). Spring resolves `{key}` placeholders in Bean Validation
messages from `ValidationMessages*.properties`, and resolves the request locale from `Accept-Language` by default.

Assumption, to verify in the backend PR: with Spring Boot 4.1's default `AcceptHeaderLocaleResolver`, adding
`ValidationMessages_es.properties` and sending `Accept-Language: es` from `apiFetch` is enough for validation
messages. `GlobalExceptionHandler` messages need a `MessageSource` lookup.

### Finding 4: Emails cannot use the request locale

`SmtpMailAdapter` sends after commit and asynchronously, so no request locale is available.

Impact: emails use the stored user preference (`users.language`). At registration, the preference is set from the
request locale, so the verification email is already in the right language.

### Finding 5: Zod messages can use keys

The schemas live in `src/lib/validation/`. With Zod 4, each message can be a translation key, which the form
resolves through `t()` when it shows the error.

Assumption: one helper that translates `errors.<field>.message` covers React Hook Form's `helperText`. Validate on
one form in PR 1.

### Finding 6: `next-intl` supports Next 16 and React 19

Version 4.14.9's peer dependencies (§2). A web report mentions a `cookies()` "outside a request scope" error in
`i18n/request.ts` under Next 16.1 (GitHub discussion amannn/next-intl#2205). Not reproduced here (risk R-002).

## 11. Proof of Concept Plan

- **Build:** `next-intl` with English and Spanish messages for `LoginForm` and `AppShell` only. The locale comes from the `brewdeck-locale` cookie; `formatDate` reads the active locale.
- **Not built:** backend, preference storage, other screens.
- **Validates:**
  - no English flash with the cookie set to `es`
  - typed keys catch a misspelled key at type-check
  - the R-002 `cookies()` issue does not occur on 16.3
  - existing tests stay green in English
- **Time-box:** 1 session. Done in a throwaway branch or as the start of PR 1.

## 12. Proof of Concept Results

TODO: POC not executed yet.

## 13. Trade-Off Analysis

| Trade-Off | Benefit | Cost |
| --- | --- | --- |
| Library vs in-house | Plurals, formatters, typed keys for free | A dependency to keep updated |
| No locale routing | No route or auth changes | URLs are not language-specific |
| Cookie + backend preference | No flash; follows the user across devices | Two copies to keep in sync (`/me` wins) |
| Feature flag | Spanish ships in pieces without half-translated screens | Flag work and later cleanup |

## 14. Risks

| Risk | Impact | Likelihood | Mitigation |
| --- | --- | --- | --- |
| R-001: Strings missed during extraction | Mixed-language screens | Medium | ESLint rule against JSX literals (`react/jsx-no-literals`) in `src/components` after extraction; Spanish typed against the English file so missing keys fail type-check |
| R-002: `next-intl` `cookies()` request-scope error on Next 16 | Locale read fails on the server | Low–Medium | POC checks it on 16.3; fallback: read the cookie in the root layout and pass `locale` + messages to the provider explicitly |
| R-003: Tests break when strings move to message files | Large test churn | Medium | Tests render with an English provider helper (like `renderWithTheme`), so visible text is unchanged |
| R-004: Backend and frontend validation messages drift | Different wording per layer | Medium | Same keys and wording reviewed together; the backend message is only shown when the client-side check passed |
| R-005: Spanish quality | Awkward copy | Medium | The owner (native speaker) reviews the Spanish file |

## 15. Recommendation

Recommended option: **`next-intl` without locale routing**. The locale comes from a `brewdeck-locale` cookie,
and the per-user `users.language` preference is the source of truth. The backend learns the language from
`Accept-Language` for requests and from the stored preference for emails.

- It is built for the App Router and covers translation, plurals, and date/number formatting, so `UI_LOCALE` becomes the active locale.
- `react-i18next` would also work but needs more wiring for the server render and typed keys.
- An in-house dictionary saves a dependency but rebuilds plurals and formatting.
- The preference mirrors the theme: nullable column, `/me` field, dedicated `PUT`, browser copy (a cookie here instead of `localStorage`, because of Finding 2).
- **Locale before any choice:** the browser language (`Accept-Language`), if it is Spanish, else English.

Validate next: the POC (§11), especially R-002 and the no-flash render.

## 16. Decision

Decision pending (owner review).

## 17. Next Steps

PR plan (each PR complete, tests included):

1. **`feat(web)`: i18n foundation, English only.**
   - Add `next-intl` and `messages/en.json`, and extract every UI string and Zod message to keys.
   - `formatDate`/`formatDateTime` take the active locale; `<html lang>` comes from the locale.
   - Add the test render helper and the `react/jsx-no-literals` rule.
   - No visible change, so no flag.
2. **`feat(api)`: language preference and localized messages.**
   - V23 `users.language VARCHAR(5)` (nullable, `CHECK IN ('EN','ES')`), plus `language` in `/me`.
   - `PUT /api/auth/me/language`, guarded by the flag.
   - `ValidationMessages.properties` + `_es`, and `GlobalExceptionHandler` through `MessageSource`.
   - Registration stores the request locale.
3. **`feat(api)`: localized emails.** Verification and reset emails in the user's language.
4. **`feat(web)`: Spanish.**
   - `messages/es.json`.
   - `apiFetch` sends `Accept-Language`.
   - Language selector in Account, and language + theme together in the first-login dialog (spike §17 row 8).
   - All behind the flag.
5. **Flag removal** once Spanish is complete and reviewed.

Feature flag (Q-007): `web-i18n-spanish`, type RELEASE.
- **Owner:** Juan.
- **Enabled in:** `local` and `dev` first; prod off until PR 4 is reviewed.
- **Expiration:** 2026-12-01.
- **Removal condition:** Spanish complete, owner-reviewed, enabled in prod.
- **Backend:** `PUT /api/auth/me/language` checks it with `requireEnabled`, so the backend stays the source of truth. Spanish validation messages need no check: they only appear when a client sends `Accept-Language: es`, and that is harmless.

Docs: an FDD for the language preference (Account selector, dialog), a TDD for the backend part, and the
API docs, `openapi.yaml`, and Postman updated for the new endpoint.

## 18. ADR Candidate

- ADR needed: Yes.
- Suggested title: "ADR-015: Internationalization with next-intl and a per-user language preference".
- Reason: it adds a long-lived frontend dependency and conventions (message files, no locale routing, cookie), plus backend locale resolution.

## 19. Open Questions

- OQ-001: Should Spanish validation messages from the backend use the same wording as the Zod messages, kept by hand, or should the frontend always prefer its own message per field?
- OQ-002: Recipe PDF: follow the user's language? (Recommended yes; it already uses `formatDate`.)
- OQ-003: Public share page (`/share/[token]`): viewer's browser language, or the owner's language?
- OQ-004: Do existing users see the first-login dialog again to pick a language, or default to their browser language until they change it in Account?

## 20. Assumptions

- Assumption-001: About 250 frontend strings + about 60 Zod messages + about 54 backend messages + 2 emails. A rough count, it sets the PR 1 size.
- Assumption-002: Spring's default `Accept-Language` resolution covers validation messages (Finding 3).
- Assumption-003: The `react/jsx-no-literals` rule is usable with the current ESLint flat config.
- Assumption-004: `next-intl` adds a modest client bundle cost. TODO: measure it in the POC with `pnpm build`.
