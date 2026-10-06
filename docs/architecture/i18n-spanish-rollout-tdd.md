# Technical Design Document: Spanish UI Rollout (i18n PR 4)

Decision record: [ADR-015](../decisions/ADR-015-i18n-next-intl-language-preference.md). Plan: the
[i18n spike](../product/spikes/i18n-spike.md) §17, PR 4. Status: implemented on `feat/web-spanish-ui`.

## 1. Summary

PRs 1–3 extracted every string, added the per-user `users.language` preference, and localized
API messages and emails. The web app still serves English only (`ENABLED_LOCALES = ['en']`). This
PR lets users pick Spanish when the `web-i18n-spanish` flag is on: a language setting in Account,
language and theme together in the first-login dialog, and a Spanish server render with no English
flash.

## 2. The problem: the server render must know the flag

The locale is resolved per request in `src/i18n/request.ts`, on the Next server, before any user
is known. The existing `GET /api/feature-flags` is authenticated and per user, and the session
lives in the browser (ADR-013). So the server render cannot read the flag today.

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| Public flag endpoint, cached on the Next server | Backend stays the source of truth (Feature Flag Policy); one switch flips Spanish everywhere | One backend call per cache window | **Chosen** |
| Environment variable on the web app | No backend change | A second switch that can disagree with the flag; needs a redeploy to change | Rejected |
| Trust the cookie only | No backend change | Pre-login pages could not detect a Spanish browser; a stale cookie would keep Spanish after the flag goes off | Rejected |

## 3. Design

### 3.1 Backend: public UI-language flag

- `GET /api/public/ui-config` (permitted by the existing `/api/public/**` rule) returns
  `{"languages": ["en"]}` or `{"languages": ["en", "es"]}`.
- It reads `web-i18n-spanish` for the environment, with no user context. That is enough because the
  flag is a plain on/off RELEASE flag with no per-user rollout.
- It is a separate endpoint, not an unauthenticated `/api/feature-flags`, so no other flag leaks to
  anonymous callers.

### 3.2 Web: locale resolution

- `src/i18n/enabledLocales.ts` fetches `/api/public/ui-config` from `API_PROXY_TARGET` with
  `next: { revalidate: 60 }`. With the backend's own 45 s flag cache, a flag change takes effect
  within about two minutes.
- **Fail-safe:** any error or timeout means `['en']`, so an API outage never breaks rendering.
- `resolveLocale(saved, acceptLanguage, enabled)` takes the enabled list instead of the constant.

### 3.3 Web: choosing the language

- **Cookie:** `brewdeck-locale` (path `/`, one year, `SameSite=Lax`; not httpOnly, since the client
  writes it). Changing it calls `router.refresh()`, so the server re-renders the layout and the
  messages in the new language without a full reload.
- **Account → Language:** a two-option setting like the theme setting. It saves with
  `PUT /api/auth/me/language`, then writes the cookie. It is shown only when the `i18nSpanish` flag
  is on for the user.
- **First-login dialog:** a new user (`themePreference` null) picks language and theme in one
  dialog. Picking a language applies it live. Existing users are not asked (owner decision, spike
  OQ-004).
- **Sync:** after the session restores, if `user.language` is set and differs from the cookie, the
  client rewrites the cookie and refreshes. The account wins over a device's leftover choice.
- **Logout** keeps the cookie: it is a device preference as much as an account one.
- `apiFetch` already sends `<html lang>` as `Accept-Language` (#208), so API messages follow.

## 4. Testing

- **Backend:** controller and integration tests for `/api/public/ui-config` in both flag states;
  anonymous access works.
- **Web:**
  - `resolveLocale` with each enabled list.
  - `enabledLocales` fail-safe on a fetch error.
  - The language setting saves, writes the cookie, and refreshes.
  - The dialog asks for language only when the flag is on.
  - Cookie sync when the account language differs.
- **Manual:** flag on locally, switch languages in Account and in the dialog; a Spanish browser
  logged out sees a Spanish login page; flag off brings English back within a minute.

## 5. Rollout

`web-i18n-spanish` is already on in `local` and `dev` (V24), so Spanish appears there once this
merges. Prod stays off until the owner reviews `messages/es.json` and the screens. Turning it on in
prod is a flag change, not a deploy. Flag removal is PR 5.

## 6. Verification (2026-10-06)

Local stack: backend on the `local` profile (flag on), `next start`.

| Request | Result |
| --- | --- |
| `/api/public/ui-config`, anonymous | `{"languages":["en","es"]}` |
| `/login`, `Accept-Language: es-MX`, no cookie | `<html lang="es">`, "Iniciar sesión" in the server HTML |
| `/login`, Spanish browser, cookie `en` | English (the saved choice wins) |
| `/login`, English browser, cookie `es` | Spanish |
| `/login`, `fr-FR` | English |
| `POST /api/auth/forgot-password` through the Next proxy, `Accept-Language: es` | "La validación falló" (the proxy forwards the header) |

Headless Chrome with `--lang=es-MX` renders the whole login page in Spanish, brand panel included.

## 7. Risks

| Risk | Mitigation |
| --- | --- |
| The API is slow or down during a render | 60 s cache plus a fetch timeout and the English fallback |
| A refresh after a language change loses unsaved form input | The language controls live in Account and the dialog, away from data forms |
| Mixed language after the flag is turned off | The server ignores `es` once the flag is off; the cookie stays but is no longer honored |
