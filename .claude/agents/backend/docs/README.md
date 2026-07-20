# Claude Code Spring Backend Engineer Agent

Reusable implementation subagent for Java and Spring Boot repositories such as BrewDeck and BrickDeck. It follows an approved requirement or architecture plan, implements a scoped backend change, adds tests, validates the result, and leaves the working tree ready for human review.

## Included files

```text
.claude/agents/backend/spring-backend-engineer.md
.claude/agents/backend/docs/README.md
.claude/agents/backend/docs/EXAMPLE-PROMPTS.md
.claude/agents/backend/docs/PERMISSIONS-GUIDE.md
.claude/agents/backend/docs/MANIFEST.txt
.claude/agents/backend/examples/settings.permissions.example.json
```

Claude Code scans `.claude/agents/` recursively for agent definitions. Only `spring-backend-engineer.md` carries agent frontmatter; the `docs/` subfolder holds human-facing package documentation and is ignored by the agent scanner. The agent is invoked as `spring-backend-engineer`.

## Install in one project

Copy the agent into the repository root:

```bash
mkdir -p /path/to/repository/.claude/agents/backend
cp .claude/agents/backend/spring-backend-engineer.md \
  /path/to/repository/.claude/agents/backend/
```

Recommended project structure:

```text
repository/
├── .claude/
│   └── agents/
│       ├── architecture/
│       │   └── solution-architect.md
│       └── backend/
│           └── spring-backend-engineer.md
├── api/
├── web/
└── ...
```

Commit the agent file to share it with the repository team.

## Install for every local project

```bash
mkdir -p ~/.claude/agents/backend
cp .claude/agents/backend/spring-backend-engineer.md \
  ~/.claude/agents/backend/
```

Project scope is recommended while BrewDeck and BrickDeck still have different conventions and domain rules.

## Recommended workflow

### 1. Produce the architecture plan

```text
@solution-architect Analyze how to implement [FEATURE]. Produce an implementation-ready plan and do not modify files.
```

### 2. Implement only the approved backend scope

```text
@spring-backend-engineer Implement the approved backend and database portions of the solution-architect plan. Follow existing project conventions, add relevant tests, run validation, and do not commit or push.
```

### 3. Run specialized reviews

```text
@test-quality-engineer Review the implementation and add missing behavioral tests.
@database-migration-reviewer Review the entity and Flyway changes for integrity and compatibility.
@security-auditor Review the implementation for security and dependency risk.
```

The review agents can be added later; the backend agent already creates explicit handoffs for them.

## Permission mode

The agent frontmatter declares only supported subagent fields (`name`, `description`, `tools`, `model`, `color`). It does not set `permissionMode`, `maxTurns`, or `effort`: those are not honored in subagent frontmatter and would give a false sense of enforcement. The agent therefore runs under whatever permission mode the session already uses (`default` unless you change it), which asks for approval before the first file modification or non-read-only shell command.

Enforceable guardrails belong in Claude Code settings, not the agent file. The package includes an optional permission-rules example at `.claude/agents/backend/examples/settings.permissions.example.json`. Review and merge only the rules that fit your operating system and repository. See `PERMISSIONS-GUIDE.md`.

## Verify the installation

From the repository root:

```bash
claude --version
claude
```

Then invoke:

```text
@spring-backend-engineer Inspect this repository and implement the approved [FEATURE] backend plan. Do not commit or push.
```

You can also run a full session under the agent:

```bash
claude --agent spring-backend-engineer
```

If the agent is not detected after creating `.claude/agents/` for the first time, restart Claude Code.

## What the agent implements

- Spring REST endpoints
- Request and response DTOs
- Validation and error handling
- Application and domain services
- JPA entities and repositories
- PostgreSQL constraints and indexes
- New Flyway migrations
- External HTTP integrations
- Unit, controller, repository, and integration tests
- Directly coupled API or configuration documentation

## What the agent does not do

- Commit, push, merge, tag, or release
- Read secrets or `.env` files
- Modify released Flyway migrations
- Run destructive Git, database, Docker, or filesystem operations
- Upgrade Spring Boot or broad dependency sets without explicit approval
- Silently change public APIs, security, or data semantics
- Implement unrelated frontend work

## Expected final report

The agent reports:

1. Implementation status
2. Behavior implemented
3. Files created and modified
4. API and database impact
5. Exact validation commands and outcomes
6. Assumptions, warnings, and pre-existing failures
7. Handoffs for testing, migration, security, documentation, or frontend review

## Suggested first use

Start with a small, representative feature rather than a broad refactor. Good candidates are:

- BrewDeck: a `BrewMethod` endpoint or one slice of `BrewSession`
- BrickDeck: one Rebrickable import improvement with deterministic tests

This makes it easier to tune project-specific rules before granting the agent larger tasks.
