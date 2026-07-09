# ADR-001: Modular monolith, package-by-domain

## Status
Accepted

## Date
2026-07-09

## Context
BrewDeck is a single-developer project that needs clear boundaries without the operational cost of microservices. Early code could drift toward package-by-layer (`controllers/`, `services/`, `repositories/`), which scatters a feature across the tree.

## Decision
Build a modular monolith organized **package-by-domain**. Each domain (`coffee`, `method`, `recipe`, `session`, `dashboard`, `ai`, `auth`) owns its controller, service, repository, entity, DTO records, filter/specification, and mapper. Cross-cutting concerns live in `common`; integration tests in `integration`.

## Consequences
- **Positive:** high cohesion, easy to locate a feature, easy to extract a module later.
- **Positive:** DTO/entity separation is enforced per domain.
- **Negative:** some duplication of small patterns across domains.
- **Negative:** shared logic must be deliberately promoted to `common` to avoid coupling.

## Alternatives Considered
- **Package-by-layer** — rejected: weaker cohesion, changes fan out across folders.
- **Microservices** — rejected: unnecessary operational overhead for a single-user app.

## Notes
Revisit if per-user, multi-tenant scaling makes a module a candidate for extraction.
