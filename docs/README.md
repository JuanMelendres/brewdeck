# BrewDeck Documentation

Docs-as-code for BrewDeck. Everything here lives in the repo and evolves with the code.

## Map

| Area | What's inside |
| ---- | ------------- |
| [product/](product/) | Vision, roadmap, feature list, and lightweight FDDs |
| [architecture/](architecture/) | System overview, technical design, database & API design, diagrams |
| [decisions/](decisions/) | Architecture Decision Records (ADRs) |
| [api/](api/) | Endpoint overview, conventions, OpenAPI, Postman pointer |
| [testing/](testing/) | Testing strategy, plan, unit & integration guides |
| [development/](development/) | Local setup, env vars, coding standards, contribution guide, [Claude Code agents](development/agents.md) |
| [security/](security/) | Vulnerability audit reports and remediation tracking |

## Related sources (not duplicated here)

- [`.claude/roadmap.md`](../.claude/roadmap.md) — phase-by-phase working roadmap (source of truth)
- [`.claude/project-state.md`](../.claude/project-state.md) — current state, recently shipped, next steps
- [`.claude/conventions.md`](../.claude/conventions.md) — working conventions
- [`docs/superpowers/plans/`](superpowers/plans/) & [`docs/superpowers/specs/`](superpowers/specs/) — per-feature plans and design specs (historical FDD/TDD detail)
- [`../CLAUDE.md`](../CLAUDE.md) — full stack/architecture rules for AI-assisted development

## Conventions

- Keep documents short, factual, and aligned with the code.
- Mark unknowns as `TODO` and guesses as `Assumption`.
- Prefer linking to a single source over duplicating content.
