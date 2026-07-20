# Claude Code Test Quality Engineer Agent

Reusable independent testing and quality-review subagent for Java and Spring Boot repositories such as BrewDeck and BrickDeck. It reviews implemented behavior, builds a risk-based test plan, adds or improves automated tests, executes validation, and reports concrete evidence for human review.

## Included files

```text
.claude/agents/quality/test-quality-engineer.md
.claude/agents/quality/docs/README.md
.claude/agents/quality/docs/EXAMPLE-PROMPTS.md
.claude/agents/quality/docs/PERMISSIONS-GUIDE.md
.claude/agents/quality/docs/MANIFEST.txt
.claude/agents/quality/examples/settings.permissions.example.json
```

Claude Code scans `.claude/agents/` recursively for agent definitions. Only `test-quality-engineer.md` carries agent frontmatter; the `docs/` and `examples/` subfolders hold human-facing package material and are ignored by the agent scanner. Invoke the agent as `test-quality-engineer`.

## Install in one project

Copy the agent into the repository root:

```bash
mkdir -p /path/to/repository/.claude/agents/quality
cp .claude/agents/quality/test-quality-engineer.md \
  /path/to/repository/.claude/agents/quality/
```

Recommended project structure:

```text
repository/
├── .claude/
│   └── agents/
│       ├── architecture/
│       │   └── solution-architect.md
│       ├── backend/
│       │   └── spring-backend-engineer.md
│       └── quality/
│           └── test-quality-engineer.md
├── api/
├── web/
└── ...
```

Commit the agent file if you want the project team to share and version it.

## Install for every local project

```bash
mkdir -p ~/.claude/agents/quality
cp .claude/agents/quality/test-quality-engineer.md \
  ~/.claude/agents/quality/
```

Project-level installation is recommended initially because BrewDeck and BrickDeck have different domain rules, test infrastructure, and acceptance criteria.

## Recommended workflow

### 1. Produce an implementation-ready plan

```text
@solution-architect Analyze how to implement [FEATURE]. Do not modify files.
```

### 2. Implement the approved backend work

```text
@spring-backend-engineer Implement the approved backend and database scope. Add baseline tests, run validation, and do not commit or push.
```

### 3. Perform an independent quality review

```text
@test-quality-engineer Review the implementation against the approved plan and acceptance criteria. Add missing high-value tests without changing production behavior. Run relevant validation and report exact evidence. Do not commit or push.
```

### 4. Hand specialized concerns to other agents

```text
@database-migration-reviewer Review persistence and Flyway compatibility.
@security-auditor Review security and dependency risks.
@documentation-writer Update test strategy and API documentation.
```

## Permission mode

The agent frontmatter declares only supported subagent fields (`name`, `description`, `tools`, `model`, `color`). It does not set `permissionMode`, `maxTurns`, or `effort`: those are not honored in subagent frontmatter and would give a false sense of enforcement. The agent runs under the session's permission mode; keep it at `default` so it requests approval before writing tests or executing non-read-only shell commands instead of silently receiving unrestricted filesystem or Bash access.

Enforceable guardrails belong in Claude Code settings, not the agent file. The package ships an optional permission example under `.claude/agents/quality/examples/` so installing it does not overwrite your real `.claude/settings.json`. See `PERMISSIONS-GUIDE.md`.

## Verify the installation

From the repository root:

```bash
claude --version
claude
```

Then invoke:

```text
@test-quality-engineer Inspect the current implementation and test suite. Identify the highest-risk missing scenarios, add the necessary tests, run validation, and do not modify production behavior.
```

You can also start a session under the agent:

```bash
claude --agent test-quality-engineer
```

Restart Claude Code when `.claude/agents/` was created after the current session started and the agent is not detected.

## What the agent reviews

- Unit-test quality and behavioral assertions
- Controller and API contract tests
- Repository mappings and custom queries
- PostgreSQL and Flyway integration
- Transaction rollback and data integrity
- External service failures, retries, and rate limits
- Idempotency and duplicate requests
- Regression risks
- Test discovery and CI execution
- Flakiness, shared state, time, and randomness

## What the agent can edit

By default, it should edit:

- Test source files
- Test fixtures and builders
- Test resources
- Local deterministic stubs
- Test-only configuration

It must not change production behavior unless the user explicitly authorizes defect remediation.

## What the agent does not do

- Commit, push, merge, tag, release, or deploy
- Read secrets or `.env` files
- Execute tests against production or unknown shared databases
- Disable or weaken failing tests
- Modify released Flyway migrations
- Change API, security, or business rules merely to make tests pass
- Introduce broad testing frameworks without approval
- Treat coverage percentage as proof of correctness

## Expected final report

The agent returns:

1. Quality status
2. Scope and risk assessment
3. Tests created or changed
4. Confirmed defects and evidence
5. Exact validation commands and results
6. Covered, partially covered, and untested behavior
7. Production files changed, normally none
8. Pre-existing failures and limitations
9. Recommended agent handoffs

## Suggested first use

Use the agent after a small implementation to calibrate its behavior:

- BrewDeck: review a `BrewMethod` endpoint or the first `BrewSession` slice
- BrickDeck: review Rebrickable set import and cache behavior

A focused first use makes it easier to tune repository-specific testing conventions before assigning a large regression suite.