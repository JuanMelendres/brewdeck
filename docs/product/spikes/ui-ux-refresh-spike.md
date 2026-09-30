# Technical Spike: UI/UX Refresh — Design Foundation

## 1. Summary

The web app works but feels flat and "boxy": it runs on almost-default MUI with no design system. This spike
records the diagnosis, compares three visual directions, and defines a proof of concept (POC) that sets the
design foundation before any screen-level work. Its output feeds the FDDs and TDD listed in §17.

- **Main question:** which visual direction and which theming architecture give BrewDeck a warmer, more
  polished UI without rewriting components or breaking behavioral tests?
- **Expected decision:** one visual direction + the theme/token approach, validated by the POC.

## 2. Background

- Frontend: Next.js (App Router), React 19, MUI 9, Emotion, TanStack Query, recharts
  (see [CLAUDE.md](../../../CLAUDE.md) Frontend Stack).
- Browser-tested on 2026-09-30 (session flow, email verification). Functionally sound; visually plain.
- Visual mockup of the three directions (private canvas, owner access):
  <https://claude.ai/artifact/49CW4qenY3kxZAmsAGV5oC>. It uses sample data, not real figures.

## 3. Problem Statement

The UI lacks a design system, so every screen inherits MUI defaults: generic typography, flat white
surfaces, 4 px radii, no hierarchy, no brand. Improving screens one by one without a foundation would produce
inconsistent, patched styling.

## 4. Goals

- Diagnose why the UI feels flat, with repository evidence.
- Compare visual directions and pick one.
- Define the theming architecture (tokens, typography, component overrides, dark mode).
- Validate it with a time-boxed POC on a few representative screens.
- Break the rest of the refresh into small, shippable workstreams.

## 5. Non-Goals

- No backend or API changes.
- No redesign of flows or information architecture beyond navigation polish.
- No new component library; MUI stays.
- Not every screen is restyled in the POC.

## 6. Key Questions

- Q-001: Which visual direction (§8) fits BrewDeck?
- Q-002: Can MUI 9 `colorSchemes` + CSS variables give light/dark mode with no flash on load under the App Router?
- Q-003: How much of the "flat" feeling does the theme alone fix, before any per-screen change?
- Q-004: Which fonts, loaded through `next/font`, and at what performance cost?
- Q-005: Do the existing Vitest + RTL tests survive the restyle unchanged (they query by role and text)?

## 7. Constraints

- Keep MUI, Emotion, and the current component structure (`src/components/<feature>/`).
- Accessibility: WCAG 2.2 AA contrast (4.5:1 body text) in both light and dark; visible focus; 44 px targets.
- Behavioral tests must keep passing; restyle must not change roles or accessible names.
- Small incremental PRs (project rule); `pnpm lint:fix` scoped to changed files.
- No feature flag needed: purely visual, each PR is complete and has no backend side effects
  (evaluated against the [Feature Flag Policy](../../../CLAUDE.md)).

## 8. Options Considered

All three are drawn in the mockup on the same dashboard.

| Option | Direction | Palette | Typography | Shape |
| --- | --- | --- | --- | --- |
| A | Warm café | Latte background `#F6EFE6`, cream surfaces, espresso `#5B3A29` | Serif display (Fraunces) + DM Sans | 16–20 px radii, soft shadows |
| B | Modern minimal | Neutral `#FAFAF9`, white surfaces, hairline borders, one accent `#7C4A2D` | Geist + Geist Mono for numbers | 8–12 px radii, borders instead of shadows |
| C | Dark premium | Espresso `#14100D`, dark surfaces, gold accent `#D4A55A` | Instrument Serif display + Manrope | 14–20 px radii, pill buttons |

Assumption: whichever is chosen, the other mode (light or dark) is derived from the same tokens, so dark mode
exists in every option.

Options B and C are kept for future reference (full tokens in Appendix A, drawings in the mockup canvas).
C's palette is reused as the dark mode of the chosen direction (§16).

## 9. Evaluation Criteria

| Criterion | Weight |
| --- | --- |
| Brand fit (coffee, craft, warmth) | High |
| Readability of dense data (tables, stats) | High |
| Accessibility (contrast in both modes) | High |
| Implementation cost with MUI theming | Medium |
| Longevity (does not age quickly) | Medium |

## 10. Research Findings

### Finding 1: The theme is almost empty

`brewdeck-web/src/lib/theme/theme.ts` defines only `palette.primary.main = '#6f4e37'`. No typography,
shape, background, shadows, or component overrides. This is the main cause of the flat look.

### Finding 2: No font is loaded

`src/app/layout.tsx` loads no font, so text falls back to the browser's Helvetica/Arial.

### Finding 3: Navigation has no state or responsiveness

`src/components/layout/AppShell.tsx`: text-only nav items, no active-route highlight, a permanent 220 px
drawer (no mobile variant), and a plain "Logout" text button.

### Finding 4: Dashboard has no hierarchy

`DashboardView` renders six identical text-only `StatCard`s; ranking widgets are tables inside outlined
cards. `recharts` is already a dependency but is not used on the dashboard.

### Finding 5: Lists and states are bare

Tables (for example `CoffeesTable`) show roast, process, and ratings as plain text. `EmptyState` is a
single grey line; loading is a full-page `Spinner` rather than skeletons.

### Finding 6: Auth screens are unbranded

`LoginForm` is a 400 px form on a white page with unstyled `<a>` links.

### Finding 7: No dark mode

No `colorSchemes`, `useColorScheme`, or dark palette anywhere in `src/`.

## 11. Proof of Concept Plan

- **What will be built:**
  - Theme foundation in `src/lib/theme/`: palette tokens (light + dark), typography scale, shape,
    shadows, component overrides (Card, Button, Table, Chip, TextField, Drawer, ListItemButton).
  - Fonts via `next/font` in `src/app/layout.tsx`.
  - Applied to three representative screens: `AppShell` (nav with icons and active state), `DashboardView`
    (stat cards with icons, one recharts chart), and `LoginForm`.
  - Light/dark toggle.
- **What will not be built:** other screens, snackbars, animations, empty-state illustrations.
- **Files likely affected:** `src/lib/theme/*`, `src/app/layout.tsx`, `src/app/providers.tsx`,
  `src/components/layout/AppShell.tsx`, `src/components/dashboard/*`, `src/components/auth/LoginForm.tsx`.
- **Expected result:** the three screens match the chosen direction; everything else improves from the
  theme alone.
- **Validation:** `pnpm test`, `pnpm type-check`, `pnpm lint`, `pnpm build`; manual check in both modes at
  1280 px and 390 px; contrast check on text and buttons.
- **Time-box:** TODO (suggested: 1–2 sessions).

## 12. Proof of Concept Results

TODO — fill in after the POC (what worked, what failed, screenshots, Q-002 to Q-005 answers).

## 13. Trade-Off Analysis

- **A (warm café):** strongest brand identity; serif headings need care in dense tables (keep serif for
  headings only).
- **B (modern minimal):** safest and most legible for data; least distinctive, risks feeling generic.
- **C (dark premium):** most striking; dark-first makes contrast and charts harder, and light mode becomes
  the secondary case.

## 14. Risks

- Restyle changes accessible names or roles and breaks tests. Mitigation: keep markup semantics; run the
  suite per PR.
- Dark mode flashes on first load under SSR. Mitigation: CSS-variables theme + `InitColorSchemeScript`
  (Assumption: available in MUI 9, validate in the POC).
- Custom fonts hurt load time. Mitigation: `next/font` with subsets and `display: swap`; at most two families.
- Palette fails contrast in one mode. Mitigation: check every text/background pair before merging.

## 15. Recommendation

Foundation first (theme + fonts + dark mode) in one PR, then the screen workstreams.

**Theme preference.** The owner wants the light/dark switch in Account settings rather than in the app bar, to
keep the main UI uncluttered, and a one-time choice right after the first login (after email verification
when it is required): a dialog asks "Light or dark?", switches the whole app live as the user picks so they can
see it, and saves the answer. Recommendation for storage: a per-user `theme_preference` on the backend
(`LIGHT` / `DARK`, `null` = never asked), exposed in `/api/auth/me` and updated through `PATCH /api/auth/me`.
That way the prompt shows once per user rather than once per browser, and the choice follows the user
across devices. Assumption: a local cached copy (localStorage) applies the theme before `/me` resolves, to
avoid a flash of the wrong theme. Alternative: localStorage only (no backend change), but the prompt would
reappear on every new device.

## 16. Decision

- Decision: **Direction A (warm café) for light mode; dark mode uses Direction C's palette (dark premium).**
  Light is the default. The toggle lives in Account settings; a first-login dialog lets the user try both and
  pick one.
- Date: 2026-09-30
- Owner: Juan (product owner)
- Status: Accepted (direction). Theme-preference storage: Proposed, see §15.

## 17. Next Steps

The refresh is split into workstreams. Each becomes its own document and PR series once the POC is accepted.

| # | Workstream | Document | Scope |
| --- | --- | --- | --- |
| 1 | Design foundation | TDD (theme architecture) + this POC | Tokens, typography, shape, shadows, component overrides, dark mode |
| 2 | Navigation | FDD | Icons, active route, logo, user menu with avatar, responsive drawer |
| 3 | Dashboard | FDD | Greeting header, stat cards with icon and color, method-usage chart, rankings as lists with ratings |
| 4 | Lists | FDD | Coffee cards or airier tables, roast/process chips, `Rating` stars, skeleton loading, `EmptyState` with icon and CTA |
| 5 | Auth screens | FDD | Split layout with brand panel, form in a card |
| 6 | Micro-interactions | FDD | Hover transitions, snackbar feedback after create/edit/delete |
| 7 | Theme preference | FDD + TDD (backend field) | Toggle in Account settings, first-login light/dark dialog with live preview, per-user persistence |

## 18. ADR Candidate

- ADR needed: Yes, once the direction is chosen.
- Suggested title: "ADR-014: UI design system on MUI theme tokens"
- Reason: sets long-term frontend conventions (tokens, fonts, dark mode) that every screen follows.

## 19. Open Questions

- ~~Which direction (Q-001)?~~ Answered: A for light, C's palette for dark (§16).
- ~~Follow the OS setting or default to light?~~ Answered: default light; the user chooses on first login and in Account settings.
- Theme preference stored per user on the backend, or localStorage only (§15)?
- Is there a logo or brand mark, or should the POC use a placeholder?

## Appendix A: Direction tokens (kept for future reference)

| Token | A · Warm café (light, chosen) | B · Modern minimal (not chosen) | C · Dark premium (chosen for dark mode) |
| --- | --- | --- | --- |
| Background | `#F6EFE6` | `#FAFAF9` | `#14100D` |
| Sidebar | `#EFE4D6` | `#FFFFFF` + border `#E7E5E4` | `#0F0C0A` + border `#2A221C` |
| Surface | `#FFFBF6` | `#FFFFFF` | `#1E1814` |
| Border / divider | `#E8DCCD` | `#E7E5E4` | `#2A221C` |
| Text primary | `#2B1D14` | `#1C1917` | `#F3EAE0` |
| Text secondary | `#6B5646` | `#57534E` | `#A89888` |
| Primary | `#5B3A29` (text on it `#FFF8EF`) | `#7C4A2D` (text on it `#FFFFFF`) | `#D4A55A` (text on it `#1A130E`) |
| Accent / tint | `#8A5A36`, tint `#F1E3D3` | `#F5F5F4` | `#E2B86E`, muted bar `#4A3A2C` |
| Display font | Fraunces 500–600 | Geist 600 | Instrument Serif 400 |
| Body font | DM Sans 400–600 | Geist 400–500 (+ Geist Mono for numbers) | Manrope 400–700 |
| Radius | 12 (controls) / 16–20 (cards) | 8 (controls) / 12 (cards) | 12 (controls) / 16–20 (cards), pill buttons |
| Elevation | Soft shadows | Hairline borders, no shadows | Borders on dark surfaces |

Assumption: the values come from the mockup and still need a WCAG AA contrast check in the POC.
