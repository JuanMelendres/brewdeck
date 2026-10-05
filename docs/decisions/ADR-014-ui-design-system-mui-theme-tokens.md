# ADR-014: UI design system on MUI theme tokens

## Status
Accepted.

## Date
2026-10-05 (direction chosen 2026-09-30; foundation built in PR #173 on 2026-10-01)

## Context
Until Phase 7 the web client ran on near-default MUI: one primary color, no font, 4 px radii, no dark mode.
The UI felt flat, and restyling screens one by one would have produced inconsistent styling. The
[UI/UX refresh spike](../product/spikes/ui-ux-refresh-spike.md) compared three visual directions and proved a
theming approach in a POC. Every screen workstream since then has been built on that foundation, so the
conventions need a durable record. Design detail: [design foundation TDD](../architecture/design-foundation-tdd.md).

## Decision
- **One MUI theme is the design system.** `brewdeck-web/src/lib/theme/theme.ts` holds palette, typography,
  shape, elevation, and component overrides. No second styling system (no Tailwind, no CSS modules for theming).
- **Visual direction:** light mode = direction A (warm café: latte background, cream surfaces, espresso
  primary); dark mode = direction C's palette (espresso background, gold primary). Light is the default.
- **Dark mode via CSS variables:** `cssVariables: { colorSchemeSelector: 'class' }` with
  `colorSchemes.light`/`dark`, plus `InitColorSchemeScript` in the root layout, so the mode is applied before
  hydration with no flash. Which mode a user gets is decided by the theme-preference workstream
  ([FDD](../product/fdd/theme-preference-fdd.md)).
- **Fonts via `next/font`:** Fraunces (display, headings only) and DM Sans (body), exposed as CSS variables
  that the theme reads, so the theme stays importable in tests.
- **Tokens, not literals:** components use palette keys and shared radius/elevation/motion tokens; no raw
  colors in components. Every text/background pair meets WCAG 2.2 AA (4.5:1) in both modes, guarded by a test.

## Consequences
- **Positive:** one place to change the look; dark mode for free on every screen that uses palette keys;
  behavioral tests are unaffected by restyling because they query by role and text.
- **Positive:** directions B and C stay available as token sets (spike Appendix A) if the brand changes.
- **Negative:** tokens live in TypeScript, so they are not shareable with non-JS tooling.
- **Negative:** `InitColorSchemeScript` is inline; a future strict CSP needs a nonce or hash for it.
- **Negative:** two web fonts add download weight (mitigated by `next/font` self-hosting, subsetting, and `display: swap`).

## Alternatives Considered
- **Direction B (modern minimal) or C (dark premium) for light mode:** B was the most legible but generic;
  C made light mode secondary and contrast harder. See spike §13.
- **Separate `ThemeProvider` per mode without CSS variables:** flashes the light theme on a dark reload under SSR.
- **Design-token pipeline (Style Dictionary) or Tailwind:** extra tooling or a second styling system for a single web app.
