# Generic UX and accessibility prompts

## Design an approved feature

```text
@ux-accessibility-designer Create an implementation-ready UX specification for
[FEATURE].

Define the primary journey, alternative and failure paths, information
architecture, screens, component states, forms, loading, empty, errors,
responsive behavior, keyboard interaction, focus, screen-reader behavior,
content, and UX acceptance criteria.

Do not modify frontend source code.
```

## Audit an existing interface

```text
@ux-accessibility-designer Audit [SCOPE] for usability, accessibility,
responsive behavior, error recovery, content clarity, and consistency.

Separate blocking accessibility defects from non-blocking usability
improvements.

Produce exact frontend handoffs.
```

## Design a form

```text
@ux-accessibility-designer Specify the [FORM] experience.

Define field order, labels, help text, units, validation timing, field and form
errors, error summary, focus, submission, duplicate prevention, unsaved changes,
success, retry, mobile layout, and accessibility.
```

## Design a data-heavy page

```text
@ux-accessibility-designer Design the [LIST OR TABLE] experience.

Cover search, filters, sorting, pagination, loading, empty results, errors,
large datasets, row actions, bulk actions, responsive alternatives, keyboard,
screen readers, and direct-link behavior.
```

# BrewDeck UX prompts

## BrewSession capture

```text
@ux-accessibility-designer Design the BrewDeck BrewSession capture experience.

Optimize for mobile use while brewing.

Cover:
- selected recipe
- planned versus actual values
- dose, water, temperature, time, and yield with units
- grind setting
- sensory ratings
- tasting notes
- validation
- interrupted submission
- preserved values
- success
- comparison with recipe
- accessibility
```

## Recipe editor

```text
@ux-accessibility-designer Design BrewDeck recipe create and edit flows.

Clearly distinguish a reusable recipe from a completed BrewSession.
Define numeric fields, units, optional steps, validation, duplication, delete,
unsaved changes, responsive layout, keyboard behavior, and error recovery.
```

## Brew history

```text
@ux-accessibility-designer Design BrewDeck session history and comparison.

Cover filters, date ranges, coffee, method, result summaries, empty
states, pagination, mobile cards versus desktop table, direct links, and
accessible data comparison.
```

## AI recipe suggestions

```text
@ux-accessibility-designer Design the AI recipe suggestions experience.

Show recommendation, rationale, uncertainty, source context, feedback, loading,
timeout, fallback, and confirmation before applying changes.

Do not present AI suggestions as guaranteed outcomes.
```

# BrickDeck UX prompts

## Set search

```text
@ux-accessibility-designer Design BrickDeck set search and result browsing.

Cover query entry, filters, sorting, pagination, remote images, loading, no
results, network errors, direct links, responsive result cards or tables,
keyboard interaction, and source provenance.
```

## Rebrickable import

```text
@ux-accessibility-designer Design the Rebrickable set-import experience.

Cover set-number validation, duplicate import, local cache hit, remote import,
partial data, rate limit, timeout, retry, progress, refresh, success, and
accessible status announcements.
```

## Complete theme import

```text
@ux-accessibility-designer Design the complete-theme-import progress experience.

Show queued, running, partial, complete, failed, paused, and retry states.
Make progress understandable after refresh and on mobile.

Define cancellation or absence of cancellation explicitly.
```

## User collection

```text
@ux-accessibility-designer Design BrickDeck collection management.

Cover quantities, duplicates, notes, ownership, visibility, filters, large
collections, add and remove, external refresh, responsive behavior, keyboard,
and accessible confirmation.
```

# Accessibility, content, and review prompts

## Accessibility specification

```text
@ux-accessibility-designer Create the accessibility specification for [FEATURE].

Define semantics, headings, landmarks, labels, fieldsets, keyboard interaction,
focus management, announcements, error association, table behavior, contrast,
zoom, reduced motion, touch targets, and manual test cases.
```

## UX writing

```text
@ux-accessibility-designer Write the interface content specification for
[FEATURE].

Include page titles, labels, instructions, empty states, errors, confirmations,
success messages, warnings, and recovery actions.

Use consistent domain terminology and plain language.
```

## Responsive review

```text
@ux-accessibility-designer Review [FEATURE] across narrow, intermediate, and wide
layouts.

Define behavior for navigation, forms, tables, filters, dialogs, long text,
actions, touch targets, virtual keyboard, zoom, and orientation.
```

## Implementation review

```text
@ux-accessibility-designer Review the implemented frontend against the approved
UX specification.

Identify missing states, inaccessible interaction, focus defects, responsive
regressions, unclear content, and deviations from product requirements.

Do not modify source code.
```
