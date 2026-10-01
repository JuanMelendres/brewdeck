# Feature: Navigation

Workstream 2 of the UI/UX refresh. Design context:
[`docs/product/spikes/ui-ux-refresh-spike.md`](../spikes/ui-ux-refresh-spike.md) (Finding 3, §17).

## Summary

The signed-in app has one navigation, a sidebar with the logo, section links with icons, the current section
highlighted, and a user card with Account and Log out. From the `md` breakpoint (900 px) up it is always
visible; below that it moves into a drawer opened from a top bar, so phones get the full page width.

## Problem

Before the refresh the sidebar was text-only, did not show where the user was, and took a fixed 220 px even
on a phone, leaving little room for content. The design-foundation POC (PR #173) added icons, the active
state, the logo, and the user card; the phone layout was still missing.

## Users

- **Signed-in user** on desktop, tablet, or phone.

## Functional Requirements

- FR-001: The navigation lists Dashboard, Coffees, Recipes, Favorites, Brew Methods, and Brew Sessions, each
  with an icon.
- FR-002: The current section is highlighted and marked `aria-current="page"`. The most specific match wins,
  so `/recipes/favorites` highlights Favorites, not Recipes; nested pages (`/coffees/7`) highlight their
  section.
- FR-003: A user card at the bottom shows the avatar initial and display name (or email), links to Account,
  and has a Log out button.
- FR-004: From 900 px up, the navigation is a permanent sidebar.
- FR-005: Below 900 px, a sticky top bar shows a menu button and the logo. The menu button opens the
  navigation in a drawer.
- FR-006: The drawer closes when the user picks a link, logs out, presses Escape, or taps outside it.

## Business Rules

- BR-001: Desktop and mobile show the same links and user card (one shared component), so they cannot drift.
- BR-002: The open drawer is modal. Focus moves into it, and the page behind is hidden from assistive
  technology until it closes.
- BR-003: The menu button exposes its state with `aria-expanded` and points at the drawer with
  `aria-controls`.

## User Flow

1. On a phone, the user sees the top bar and the page content at full width.
2. The user taps the menu button; the drawer slides in with the current section highlighted.
3. The user taps Coffees; the drawer closes and the Coffees page opens.

## Edge Cases

- Rotating or resizing across 900 px swaps between the sidebar and the top bar; an open drawer is hidden
  on desktop widths.
- A user without a display name sees their email in the user card.

## Out of Scope

- Collapsing the desktop sidebar to icons only.
- A bottom tab bar for phones.
- Search or quick actions in the top bar.
