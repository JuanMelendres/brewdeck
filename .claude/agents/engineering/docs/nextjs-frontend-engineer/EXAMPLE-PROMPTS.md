# Generic frontend prompts

## Implement an approved feature

```text
@nextjs-frontend-engineer Implement the frontend portion of the approved
[FEATURE] plan.

Inspect existing conventions and the closest comparable feature.
Cover loading, empty, success, validation, server error, authorization, not
found, responsive behavior, accessibility, tests, and API contract handling.

Do not modify backend code, migrations, infrastructure, secrets, or public API
contracts.
```

## Review current frontend diff

```text
@nextjs-frontend-engineer Review and complete the current frontend changes.

Identify missing UI states, TypeScript issues, accessibility problems,
contract mismatches, test gaps, performance risks, and unrelated changes.

Implement only frontend fixes.
Run the relevant lint, type-check, tests, and build commands.
Do not commit or push.
```

## Build a CRUD UI

```text
@nextjs-frontend-engineer Implement the CRUD interface for [RESOURCE].

Follow repository conventions for routes, forms, API clients, validation,
notifications, dialogs, tests, and styling.

Include list, empty state, detail, create, edit, delete confirmation, loading,
errors, unauthorized, forbidden, and not-found behavior.

Do not invent backend behavior.
```

## Frontend quality review

```text
@nextjs-frontend-engineer Audit the frontend architecture and implementation.

Review server/client boundaries, data fetching, state management, forms,
accessibility, responsive behavior, error handling, security, performance,
tests, and dependency usage.

Do not perform a redesign. Produce focused remediation.
```

# BrewDeck frontend prompts

## BrewSession interface

```text
@nextjs-frontend-engineer Implement the BrewSession frontend according to the
approved FDD, TDD, and API contract.

Include:
- recipe selection
- planned values
- actual values
- grind setting
- dose, water, temperature, time, and yield with units
- sensory ratings
- tasting notes
- validation
- submission progress
- duplicate submission prevention
- success and error feedback
- mobile and desktop layouts
- accessible form behavior
- tests

Do not modify the Spring Boot API or Flyway migrations.
```

## Recipe management

```text
@nextjs-frontend-engineer Implement BrewDeck recipe list, detail, create, and
edit experiences.

Clearly distinguish a reusable recipe from an executed brew session.
Preserve numeric precision and display units consistently.

Handle ownership, loading, empty state, validation, conflicts, not found, and
server errors.
Do not invent missing API fields.
```

## Brew history

```text
@nextjs-frontend-engineer Implement BrewDeck brew-session history.

Include recent sessions, filters, pagination, empty state, loading, errors,
accessible table or list semantics, responsive layout, and direct-link behavior.

Use URL state for shareable filters when compatible with repository
conventions.
```

## AI recipe suggestions UI

```text
@nextjs-frontend-engineer Create a feature-flagged prototype UI for AI recipe
suggestions (flag: ai_recipe_assistant) using approved mock data only.

Clearly label recommendations as suggestions.
Include user confirmation before write actions, source context, feedback,
loading, failure, retry, and safety messaging.

Do not create a production API mock that hides missing backend implementation.
```

# BrickDeck frontend prompts

## Set search and details

```text
@nextjs-frontend-engineer Implement BrickDeck set search and set-detail pages.

Include search, filters, pagination, loading, empty state, errors, remote image
handling, Rebrickable provenance, external link behavior, cache status, and
responsive accessibility.

Do not modify backend import behavior.
```

## Rebrickable import interface

```text
@nextjs-frontend-engineer Implement the BrickDeck Rebrickable set-import UI.

Cover:
- set-number input
- validation
- submission state
- duplicate prevention
- imported-from-remote result
- local-cache-hit result
- unauthorized upstream error
- rate limit
- timeout
- malformed response
- partial data
- retry
- accessible status announcements

Do not expose the Rebrickable API key in browser code.
```

## Theme import progress

```text
@nextjs-frontend-engineer Implement the frontend for complete theme imports.

Show queued, running, partially completed, completed, failed, and retry states.
Make long-running progress understandable and resilient to refresh.

Verify the backend contract before selecting polling, server events, or another
update strategy.
```

## User collection

```text
@nextjs-frontend-engineer Implement BrickDeck user-collection management.

Include ownership, add, remove, quantity, duplicate handling, private/public
visibility when supported, loading, errors, confirmation, and tests.

Do not use client-side checks as the authorization boundary.
```

# Accessibility, testing, and release prompts

## Accessibility audit and remediation

```text
@nextjs-frontend-engineer Audit and remediate accessibility issues in [SCOPE].

Review semantic structure, keyboard navigation, focus, labels, dialogs, status
announcements, errors, contrast assumptions, reduced motion, tables, and touch
targets.

Add automated tests where supported and document manual checks.
Do not change product behavior beyond necessary accessibility fixes.
```

## Responsive review

```text
@nextjs-frontend-engineer Review [FEATURE] at mobile, tablet, and desktop sizes.

Fix overflow, unusable controls, unreadable tables, long text, empty states,
loading states, and dialog behavior while preserving existing design tokens.
```

## Frontend release validation

```text
@nextjs-frontend-engineer Perform frontend release validation.

Run repository-approved lint, type-check, unit tests, component tests, E2E tests,
and production build.

Review user-critical routes, authentication expiry, API errors, direct links,
browser refresh, responsive behavior, accessibility, bundle impact, and feature
flags.

Do not commit, push, release, or deploy.
```

## API contract mismatch

```text
@nextjs-frontend-engineer Compare the frontend API client and UI assumptions
against the current backend controllers, DTOs, validation, and error responses.

Do not modify backend code.
Report exact mismatches and create a handoff for @spring-backend-engineer.
Implement safe frontend adapters only when they preserve the approved contract.
```
