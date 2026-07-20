# Claude Code agents

BrewDeck ships a set of specialized Claude Code subagents under
[`.claude/agents/`](../../.claude/agents/). They are reusable across BrewDeck and
sibling projects (e.g. BrickDeck). Claude Code scans `.claude/agents/` recursively
and registers any Markdown file that has agent frontmatter; you invoke an agent by
its `name` with `@<name>` in a prompt, or run a whole session under one with
`claude --agent <name>`.

## Grouping

Agents are grouped by lifecycle phase. The folder is organizational only — an agent
is addressed by its `name`, not its path, so moving it between folders does not change
how you call it.

| Group | Agents |
|---|---|
| `planning/` | `product-requirements-analyst`, `solution-architect` |
| `engineering/` | `spring-backend-engineer`, `nextjs-frontend-engineer`, `api-integration-engineer`, `ai-llm-engineer`, `ux-accessibility-designer` |
| `quality/` | `test-quality-engineer`, `database-migration-reviewer`, `performance-reliability-engineer`, `pull-request-reviewer` |
| `security/` | `security-auditor`, `dependency-upgrade-engineer` |
| `operations/` | `devops-platform-engineer`, `release-manager`, `incident-response-engineer` |
| `documentation/` | `documentation-writer` |

Each group folder has a `docs/README.md` index and, per agent, a
`docs/<agent-name>/` folder with `EXAMPLE-PROMPTS.md`, `PERMISSIONS-GUIDE.md`, an
`examples/settings.permissions.example.json`, and `templates/` where relevant.

## Catalog

### Planning

- **`product-requirements-analyst`** — turns vague ideas, stakeholder requests, bugs,
  or opportunities into product requirements: goals, flows, business rules, acceptance
  criteria, MVP scope, and feature-flag needs. Documentation-only.
- **`solution-architect`** — analyzes a feature/change before implementation and
  produces an evidence-based technical plan with options, trade-offs, and handoffs.
  Read-only.

### Engineering

- **`spring-backend-engineer`** — implements approved Spring Boot backend work
  (endpoints, DTOs, services, JPA, PostgreSQL, Flyway, tests).
- **`nextjs-frontend-engineer`** — implements approved frontend behavior in
  `brewdeck-web/` (pages, components, forms, all UI states, accessibility, tests).
- **`api-integration-engineer`** — resilient third-party/internal API integration
  (HTTP clients, auth, retries, idempotency, provider isolation, contract tests).
- **`ai-llm-engineer`** — AI/LLM features (RAG, tool calling, prompt/model versioning,
  structured output, evaluation, safety).
- **`ux-accessibility-designer`** — UX, interaction design, information architecture,
  and WCAG accessibility specifications.

### Quality

- **`test-quality-engineer`** — independent test/quality review; adds high-value tests
  and reports evidence.
- **`database-migration-reviewer`** — reviews DB/Flyway changes for existing-data
  compatibility, locking, rollout, and rollback. Read-only.
- **`performance-reliability-engineer`** — performance and reliability review
  (N+1 queries, latency, throughput, failure modes).
- **`pull-request-reviewer`** — final PR review across correctness, tests, security,
  performance, and docs before merge.

### Security

- **`security-auditor`** — independent security and supply-chain review with
  exploitability triage. Read-only.
- **`dependency-upgrade-engineer`** — plans and stages dependency/framework upgrades
  with compatibility analysis and validation.

### Operations

- **`devops-platform-engineer`** — Docker, Compose, GitHub Actions, CI/CD, build and
  release plumbing, environment configuration.
- **`release-manager`** — go-live readiness: scope, risk, rollout/rollback, feature-flag
  gating, and release-gate sign-off.
- **`incident-response-engineer`** — independent incident triage, troubleshooting, and
  postmortems.

### Documentation

- **`documentation-writer`** — creates/updates ADRs, FDDs, TDDs, spikes, API docs,
  runbooks, and release notes, verified against the repository. Edits documentation only.

## Recommended pipeline

For a non-trivial feature the agents chain like this (skip stages that do not apply):

```
product-requirements-analyst   →  what & why (requirements, MVP, flags)
solution-architect             →  how (architecture plan, handoffs)
ux-accessibility-designer      →  UX & accessibility specs (UI features)
spring-backend-engineer /      →  implementation
  nextjs-frontend-engineer /
  api-integration-engineer /
  ai-llm-engineer
test-quality-engineer          →  independent tests
database-migration-reviewer    →  schema/migration safety (DB changes)
performance-reliability-engineer → performance & reliability (hot paths)
security-auditor               →  security & supply-chain
dependency-upgrade-engineer    →  upgrades (when remediation needs them)
pull-request-reviewer          →  final gate before merge
release-manager +              →  rollout / go-live
  devops-platform-engineer
incident-response-engineer     →  when something breaks in an environment
documentation-writer           →  keep docs in sync (same PR)
```

## Conventions

- **Frontmatter**: only `name`, `description`, `tools`, `model`, and `color` are honored
  by Claude Code subagents. `permissionMode`, `maxTurns`, and `effort` are ignored and
  are not used. Enforceable guardrails live in each agent's
  `docs/<agent>/examples/settings.permissions.example.json` — review and merge the
  relevant rules into `.claude/settings.json`; do not copy blindly.
- **Read-only reviewers** (`solution-architect`, `database-migration-reviewer`,
  `security-auditor`, `performance-reliability-engineer`, `pull-request-reviewer`,
  `incident-response-engineer`) have no `Edit`/`Write` tools and produce reports plus
  handoffs; they never modify the branch.
- **BrewDeck fit**: BIGINT identity primary keys (not UUID); domains are coffees,
  brew methods, recipes, brew sessions, plus users, tokens, and feature flags; there is
  no grinder or e-ink feature; the AI feature is AI recipe suggestions
  (`ai_recipe_assistant`).
- No agent commits, pushes, merges, tags, releases, or reads secrets.
