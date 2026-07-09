# ADR-004: Next.js + MUI + TanStack Query frontend

## Status
Accepted

## Date
2026-07-09

## Context
BrewDeck needs a mobile-first web client that consumes the REST API with predictable data fetching, caching, and forms, using strict TypeScript.

## Decision
Build the frontend with **Next.js (App Router) + React 19 + TypeScript**, **MUI** for UI, **TanStack Query** for server state, and **React Hook Form + Zod** for forms/validation. A thin `fetch`-based API client wraps calls; tests use **Vitest + React Testing Library**.

## Consequences
- **Positive:** server state is cached/invalidated centrally; no server data in global client state.
- **Positive:** strict types and Zod schemas mirror backend validation.
- **Negative:** MUI + Emotion adds bundle weight versus a utility-first approach.
- **Negative:** App Router conventions (default exports for pages) require discipline.

## Alternatives Considered
- **Tailwind / shadcn-ui** — considered early (see older roadmap notes); MUI chosen and is what ships (`package.json`).
- **Redux Toolkit for server data** — rejected: TanStack Query is the right tool for server cache.
- **axios / Formik+Yup** — rejected in favor of `fetch` and React Hook Form + Zod.

## Notes
`page.tsx`/`layout.tsx` use `export default` (Next.js requirement); everything else uses named exports.
