# Claude Code Solution Architect Agent

Reusable, read-only solution architecture subagent for Claude Code. It is designed for repositories such as BrewDeck and BrickDeck, while remaining adaptable to future Java/Spring Boot, Next.js/React, PostgreSQL, Docker, and integration-heavy projects.

## Included file

```text
.claude/agents/architecture/solution-architect.md
```

Claude Code scans `.claude/agents/` recursively, so the `architecture/` subfolder is only organizational. The agent identity comes from `name: solution-architect`.

## Install in one project

Copy the included `.claude` directory into the repository root:

```bash
cp -R .claude /path/to/your/repository/
```

Or create the destination and copy only the agent:

```bash
mkdir -p /path/to/your/repository/.claude/agents/architecture
cp .claude/agents/architecture/solution-architect.md \
  /path/to/your/repository/.claude/agents/architecture/
```

Commit the file so the agent is shared with the project team.

## Install for every local project

```bash
mkdir -p ~/.claude/agents/architecture
cp .claude/agents/architecture/solution-architect.md \
  ~/.claude/agents/architecture/
```

Project scope is recommended initially because BrewDeck and BrickDeck may evolve different architectural rules.

## Verify

From the repository root:

```bash
claude --version
claude
```

Then invoke the agent explicitly:

```text
@solution-architect Analyze the implementation of BrewSession history and produce an implementation-ready technical plan. Do not modify files.
```

Depending on the Claude Code interface, the agent may appear in `@` typeahead as `solution-architect (agent)`. You can also ask naturally:

```text
Use the solution-architect agent to analyze this feature before implementation.
```

To run an entire session using the agent:

```bash
claude --agent solution-architect
```

## Recommended first tests

### BrewDeck

```text
@solution-architect Analyze how to implement BrewSession using the existing Coffee, BrewMethod, and Recipe design. Cover backend, frontend, Flyway, API contracts, tests, security, documentation, and agent handoff. Do not modify files.
```

### BrickDeck

```text
@solution-architect Analyze how to import complete LEGO themes from Rebrickable while preserving local caching, provenance, rate-limit handling, idempotency, and data integrity. Do not modify files.
```

## Why persistent memory is not enabled yet

Claude Code persistent agent memory automatically enables Read, Write, and Edit so the agent can maintain its memory directory. This first version intentionally prioritizes a strict read-only architecture boundary. Project memory can be introduced later after defining what architectural knowledge should be stored and how it should be reviewed.

## Expected result

The agent returns:

1. Current-state findings grounded in repository paths.
2. Architectural options and trade-offs.
3. A recommended design.
4. Data, API, security, compatibility, and operational impact.
5. An ordered implementation plan.
6. Testing and quality gates.
7. Required ADR, TDD, FDD, Spike, API documentation, or runbook decisions.
8. Handoff tasks for future specialized agents.
9. A final status: ready, ready with assumptions, spike required, or blocked.

## Important limitation

The agent is a planner and reviewer. It does not implement code, edit migrations, create documentation files, commit changes, or open pull requests.
