# Feature: Theme Preference

Workstream 7 of the UI/UX refresh. Design context and the backend design:
[`docs/product/spikes/ui-ux-refresh-spike.md`](../spikes/ui-ux-refresh-spike.md) (§15, §16).

## Summary

Each user chooses a light or dark theme once, through a dialog right after their first login, and can change
it later in Account settings. The choice is stored on the user's account, so it follows them to any device.

## Problem

The design-foundation POC (PR #173) added light and dark themes, but the choice lives only in the browser:
a new device starts in light mode, and nothing tells users dark mode exists.

## Users

- **Signed-in user:** picks and changes their theme.
- **Visitor (not signed in):** sees the last theme used in that browser on the public pages (login, register).

## Functional Requirements

- FR-001: `GET /api/auth/me` returns `themePreference`: `LIGHT`, `DARK`, or `null` when the user has not chosen.
- FR-002: `PUT /api/auth/me/theme` with `{ "themePreference": "LIGHT" | "DARK" }` saves the choice and returns
  the updated user (`200`). A missing or unknown value returns `400`; no token returns `401`.
- FR-003: When a signed-in user's `themePreference` is `null`, the web app shows a one-time dialog asking
  "Light or dark?". Picking an option switches the whole app live so the user sees it before confirming.
- FR-004: Confirming the dialog saves the choice through FR-002 and closes the dialog. It does not appear again
  for that user, on any device.
- FR-005: Account settings shows the Light/Dark selector. Changing it switches the app at once and saves
  through FR-002.
- FR-006: After sign-in, the saved preference is applied in every browser the user uses.

## Business Rules

- BR-001: Light is the default until the user chooses.
- BR-002: Existing accounts start with `null`, so they also see the dialog once.
- BR-003: Updating the theme never changes other profile fields. That is why it has its own endpoint instead
  of `PATCH /api/auth/me`, which sets the display name from the request as given.
- BR-004: When email verification is required (ADR-012), the dialog appears only once the user is verified;
  the endpoint is not on the unverified allow-list.
- BR-005: The browser keeps a copy of the last theme, so pages render in the right theme before `/me`
  resolves. The account value wins whenever it is set.

## User Flow

1. A user signs in for the first time (after verifying their email when that is required).
2. The app loads `/me`; `themePreference` is `null`, so the dialog opens with Light selected.
3. The user taps Dark; the app switches to dark behind the dialog.
4. The user confirms; the app calls `PUT /api/auth/me/theme` and closes the dialog.
5. Later, on another device, the user signs in; `/me` returns `DARK` and the app opens in dark mode.
6. In Account settings, the user switches back to Light; the change applies and is saved.

## Edge Cases

- Saving fails (network or server error): the theme stays applied in this browser, the user sees an error
  message, and because the account value is still `null`, the dialog appears again on the next sign-in.
- Two users share a browser: after sign-in, each user's saved preference replaces the browser copy.
- Signing out keeps the browser copy, so the login page keeps the last theme used.

## Out of Scope

- Following the operating system's light/dark setting.
- More themes than light and dark.
- The language preference (workstream 8, i18n); the dialog may later ask language and theme together.

## Open Questions

- Assumption: the dialog cannot be dismissed without choosing (Light is preselected, so it is one click).
  TODO: confirm with the owner.
