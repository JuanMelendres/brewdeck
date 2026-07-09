# Product Vision

## What BrewDeck is

BrewDeck is a personal coffee companion. It helps coffee enthusiasts track, optimize, and reproduce brewing recipes so they can consistently make a better cup.

## Problem it solves

Brewing knowledge is usually scattered across notes apps, chat messages, and memory. There is no structured way to answer *"how did I brew that great cup last week?"*. BrewDeck centralizes coffees, recipes, and brew sessions into one data-driven system.

## Target users

- **Home brewing enthusiasts** who tweak grind, ratio, temperature, and time and want repeatable results.
- **Coffee hobbyists** building a personal library of coffees and recipes.
- Single-user today; multi-user is in progress (see [roadmap](roadmap.md)).

## Current scope

- Manage coffees, brew methods, recipes, and brew sessions (full CRUD).
- Favorites, filtering, pagination, and a dashboard summary.
- Read-only analytics (top-rated / most-brewed recipes, most-used coffees, method usage, rating trend, tasting radar).
- AI-assisted recipe suggestions and improvement (feature-flagged).
- Opt-in public share links for recipes.
- Authentication foundation (self-registration + JWT login).

## Future scope

- Per-user ownership and account UX (email verification, password reset, refresh tokens).
- Cloud deployment.
- **Vision:** a physical e-paper companion device that syncs with the platform, plus offline sync and advanced analytics.

## Non-goals (for now)

- Social network features beyond simple share links.
- Marketplace / e-commerce.
- Native mobile apps (the web app is mobile-first instead).

> `Assumption`: target-user framing is inferred from the README and feature set; confirm if a sharper audience definition exists.
