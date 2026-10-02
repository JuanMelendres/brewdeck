# Feature: Forms

Workstream 9 of the UI/UX refresh. Design context:
[`docs/product/spikes/ui-ux-refresh-spike.md`](../spikes/ui-ux-refresh-spike.md) (§17).

## Summary

The create/edit dialogs for coffees, recipes, and brew sessions group their fields into titled sections and
use two or three columns on wider screens, so the forms are shorter and easier to scan. Select fields keep
their label above the field so it never overlaps the placeholder option.

## Problem

Found by the owner while reviewing the refreshed UI (2026-10-01):

- The native selects in the Recipe and Brew Session dialogs showed their label on top of the always-visible
  placeholder option ("Select a coffee").
- The dialogs stacked every field in one column (12 fields and 4 sliders for a coffee), which made them long
  on desktop.

## Users

- **Signed-in user** adding or editing coffees, recipes, and brew sessions, on desktop or phone.

## Functional Requirements

- FR-001: Every dialog groups its fields into titled sections by meaning:
  - **Coffee:** Coffee (name, brand, roast level, process, variety), Origin (origin, region, farm, producer),
    Tasting (primary and secondary notes, the four 1–5 score sliders, description).
  - **Recipe:** Recipe (name, coffee, brew method, Suggest with AI when enabled, favorite), Brewing (coffee
    grams, water grams, ratio, grind setting, water temp, brew time), Notes (steps, expected taste).
  - **Brew Session:** Brew (recipe, actual grind, actual temp, actual time), Result (rating, taste result,
    adjustment notes).
- FR-002: The dialogs are `md` wide. Short fields sit two per row from 600 px; recipe brewing parameters sit
  three per row from 900 px and brew session parameters three per row from 600 px. Names and long text fields
  use the full width.
- FR-003: On phones (below 600 px) every field takes the full width.
- FR-004: Multiline fields (description, steps, expected taste, taste result, adjustment notes) start three
  rows tall.
- FR-005: Select labels always sit above the field.

## Business Rules

- BR-001: Layout only. Field names, validation, required fields, and API payloads do not change.
- BR-002: Each section is a region labelled by its heading (`FormSection`), so screen readers announce it.
- BR-003: All three dialogs use the shared `FormSection` component, so sections look the same everywhere.

## Edge Cases

- The Suggest with AI button and its messages appear in the Recipe section only when the
  `aiRecipeAssistant` flag is on; the layout is the same without them.
- Validation errors show under their own field and can make one cell of a row taller than its neighbour.

## Out of Scope

- Replacing native selects with searchable autocompletes.
- Splitting the forms into multi-step wizards.

## Implementation

- PR #178: coffee dialog sections and the select-label fix.
- Recipe and Brew Session dialog sections, and the shared `src/components/ui/FormSection.tsx`.
