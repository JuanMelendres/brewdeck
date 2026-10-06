# ADR-015: Internationalization with next-intl and a per-user language preference

## Status
Accepted. Implementation pending (5 PRs, see the spike's §17).

## Date
2026-10-05

## Context
BrewDeck is English-only. Phase 7 workstream 8 adds Spanish with a per-user language preference. The web app is
an authenticated, client-rendered Next 16 App Router app (ADR-004, ADR-013), and the backend has no locale handling.
Options, findings, and the proof of concept are in the [i18n spike](../product/spikes/i18n-spike.md).

## Decision
- **Library:** `next-intl` (v4) for translations, ICU plurals and rich text, and date/number formatting
  (`useFormatter` replaces the fixed `UI_LOCALE`). Messages live in `brewdeck-web/messages/{en,es}.json`. Keys are
  typed from the English file through `AppConfig`, and a test keeps the Spanish keys in parity.
- **No locale routing.** URLs carry no language (`/es/...` is not used): the app sits behind a login and needs no SEO.
- **Locale resolution, per request:** the `brewdeck-locale` cookie, else `Accept-Language`, else English. It is read
  in `src/i18n/request.ts`, so the server render is already in the right language with no English flash.
- **Source of truth:** a nullable `users.language` (`EN`/`ES`, Flyway V23), returned in `/api/auth/me` and written by
  `PUT /api/auth/me/language`. This mirrors the theme preference. The client rewrites the cookie when `/me` differs.
  Existing users keep their browser language until they choose. New users choose in the first-login dialog,
  together with the theme.
- **Backend messages:** `apiFetch` sends `Accept-Language`, and validation and error messages resolve from
  `ValidationMessages*.properties` / `MessageSource`. The frontend prefers its own field message when it has one.
- **Emails:** sent in the user's stored language, since asynchronous sending has no request locale. Registration
  stores the request locale.
- **Rollout:** a RELEASE flag, `web-i18n-spanish`, hides Spanish until every screen is translated. The backend
  checks it on `PUT /api/auth/me/language`.
- **Supply chain:** the build scripts of `next-intl`'s optional message-extractor dependencies (`@swc/core`,
  `@parcel/watcher`) stay blocked in `pnpm-workspace.yaml`.

## Consequences
- **Positive:** one mechanism for text and formatting; typed keys catch typos at type-check; no route or auth changes.
- **Negative:** every page renders per request (they were prerendered before), because the locale is read from the
  cookie. That is acceptable: the app already runs as a Node server and loads its data client-side.
- **Negative:** about 11 KB gzip more client JavaScript, and a dependency with a wide transitive tree.
- **Negative:** every user-facing string must go through a key. An ESLint rule (`react/jsx-no-literals`) keeps new literals out.

## Alternatives Considered
- **`react-i18next`:** mature, but more wiring for the server render and typed keys; non-ICU plurals.
- **In-house dictionary:** no dependency, but plurals, rich text, and formatting would be rebuilt by hand.
- **`[lang]` route segment:** shareable localized URLs, at the cost of moving every route and changing the auth redirects, with no SEO benefit.
- **Locale only in `localStorage`:** keeps pages static but paints English first, then switches.
