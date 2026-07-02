---
name: frontend-nextjs
description: Use this skill when starting or modifying the BrewDeck frontend using Next.js, React, TypeScript, API clients, forms, pagination UI, filters, dashboard pages, or frontend architecture.
---

# Frontend Next.js Skills

## Role

Act as a senior frontend engineer using Next.js (App Router), React, TypeScript, and modern UI patterns.

## Tech Stack & Assumptions

- **Framework:** Next.js (App Router)
- **Language:** TypeScript
- **Forms:** React Hook Form + Zod (for validation)
- **Testing:** Vitest (or Jest) + React Testing Library

## Future Frontend Goals

Build a frontend for BrewDeck that consumes the Spring Boot API.

Planned screens:

- Dashboard
- Coffees list
- Coffee detail
- Coffee create/edit
- Brew methods list
- Recipes list
- Recipe detail
- Recipe create/edit
- Favorite recipes
- Brew sessions list
- Brew session create
- Brew sessions by recipe

## API Assumptions

Collection endpoints return PageResponse:

```ts
type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
```

## Frontend Rules

- Use TypeScript types for all API responses.
- Create an API client layer.
- Do not call fetch directly from every component.
- Keep forms typed and validated.
- Support pagination and filters in list pages.
- Keep UI components reusable.
- Handle loading, empty, and error states.

## Testing Rules

- Write unit tests for complex UI components, custom hooks, and utility functions.
- Use React Testing Library to test behavior and accessibility, not implementation details.
- Mock the API client layer when testing page components to avoid real network requests.

## Before Starting Frontend

Confirm backend has:

- CORS for http://localhost:3000
- Stable DTOs
- PageResponse
- Swagger available