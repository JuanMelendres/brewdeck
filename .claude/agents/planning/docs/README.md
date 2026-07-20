# Planning agents

Upstream agents that turn ideas and requirements into implementation-ready plans.
Read-only or documentation-only; they do not implement code.

| Agent | Invoke | Use for |
|---|---|---|
| `product-requirements-analyst` | `@product-requirements-analyst` | Turn vague ideas, stakeholder requests, bugs, or opportunities into product requirements: goals, user flows, business rules, acceptance criteria, MVP scope, feature-flag needs. |
| `solution-architect` | `@solution-architect` | Analyze a feature/change before implementation: current-state evidence, options and trade-offs, an implementation-ready technical plan, and agent handoffs. |

Typical order: `product-requirements-analyst` (what & why) → `solution-architect` (how) → engineering agents.

Per-agent reference material (example prompts, permission example, templates) lives in
`docs/<agent-name>/`. See the catalog in [`docs/development/agents.md`](../../../../docs/development/agents.md).
