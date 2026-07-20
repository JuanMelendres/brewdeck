# Claude Code Database Migration Reviewer Agent

Reusable, independent database and persistence review subagent for Java and
Spring Boot repositories using PostgreSQL, JPA/Hibernate, and Flyway.

It reviews migration correctness, existing-data compatibility, ORM alignment,
locking, deployment ordering, backfills, rollback limitations, and
zero-downtime risk. It does not edit source files or migrations.

## Included files

```text
.claude/agents/quality-database/database-migration-reviewer.md
.claude/agents/quality-database/docs/README.md
.claude/agents/quality-database/docs/EXAMPLE-PROMPTS.md
.claude/agents/quality-database/docs/PERMISSIONS-GUIDE.md
.claude/agents/quality-database/docs/MANIFEST.txt
.claude/agents/quality-database/examples/settings.permissions.example.json
```

Claude Code scans `.claude/agents/` recursively for agent definitions. Only
`database-migration-reviewer.md` carries agent frontmatter; the `docs/` and `examples/`
subfolders hold human-facing package material and are ignored by the agent scanner.
Invoke the agent as `database-migration-reviewer`.

## Install in one project

From the extracted package:

```bash
mkdir -p /path/to/repository/.claude/agents/quality-database

cp .claude/agents/quality-database/database-migration-reviewer.md \
  /path/to/repository/.claude/agents/quality-database/
```

Recommended structure after installation:

```text
repository/
├── .claude/
│   └── agents/
│       ├── architecture/
│       │   └── solution-architect.md
│       ├── backend/
│       │   └── spring-backend-engineer.md
│       ├── quality/
│       │   └── test-quality-engineer.md
│       └── quality-database/
│           └── database-migration-reviewer.md
├── api/
├── web/
└── ...
```

Project-level installation is recommended because BrewDeck and BrickDeck have
different schemas, data ownership rules, and migration histories.

## Install for every local project

```bash
mkdir -p ~/.claude/agents/quality-database

cp .claude/agents/quality-database/database-migration-reviewer.md \
  ~/.claude/agents/quality-database/
```

A user-level installation is useful for a common baseline, but project-specific
rules should still live in each repository's `CLAUDE.md`, skills, ADRs, or agent
copy.

## Permission model

The agent's primary safety boundary is its tool allowlist:

```yaml
tools: Read, Grep, Glob, Bash
```

It intentionally has no `Edit` or `Write` tool, so it can inspect the repository
and request approval for safe local validation commands but cannot alter
migration files, entities, tests, or configuration. The frontmatter declares only
supported subagent fields (`name`, `description`, `tools`, `model`, `color`); it does
not set `permissionMode`, `maxTurns`, or `effort`, which are not honored in subagent
frontmatter. The agent runs under the session's permission mode (`default` unless you
change it).

The package includes an optional settings example under
`.claude/agents/quality-database/examples/`. Review and merge individual rules into
your own settings rather than replacing an existing `.claude/settings.json`.

## Recommended workflow

### 1. Architecture analysis

```text
@solution-architect Analyze [FEATURE] and define the persistence, rollout, and
compatibility approach. Do not modify files.
```

### 2. Backend implementation

```text
@spring-backend-engineer Implement the approved backend and database plan.
Create a new forward Flyway migration and baseline tests. Do not commit or push.
```

### 3. Independent migration review

```text
@database-migration-reviewer Review the database changes against the approved
plan. Evaluate existing data, JPA mappings, Flyway history, locking, rollout,
backfill, and recovery. Do not modify files.
```

### 4. Independent test review

```text
@test-quality-engineer Add the missing migration, repository, and integration
tests identified by the database review. Do not commit or push.
```

## Verify installation

Start Claude Code from the repository root:

```bash
claude
```

Then invoke:

```text
@database-migration-reviewer Inspect the current database-related diff and
produce a migration safety report. Do not modify files or connect to any
non-disposable database.
```

You can also start a session using the agent:

```bash
claude --agent database-migration-reviewer
```

When `.claude/agents/` did not exist when the current Claude Code session
started, restart the session if the new agent does not appear.

## What the agent reviews

- Flyway versioning, ordering, checksums, and configuration
- Existing-data compatibility
- JPA/Hibernate versus PostgreSQL type alignment
- UUID, numeric, timestamp, enum, JSONB, and relationship mappings
- Nullability, defaults, uniqueness, foreign keys, and check constraints
- Index design and query support
- Backfill idempotency, batching, and resumability
- Locking, table rewrites, WAL, and deployment duration
- Rolling and blue/green compatibility
- Application rollback versus database recovery
- Migration testing from empty and previous schemas
- Schema drift and edited historical migrations

## What the agent does not do

- Edit source files or migrations
- Rewrite released migration history
- Run `flyway clean` or execute `flyway repair`
- Connect to production, shared staging, or unknown databases
- Run destructive SQL
- Commit, push, merge, release, or deploy
- Disable Flyway validation
- Approve a migration only because an empty database starts

## Expected report statuses

The agent ends with exactly one status:

```text
APPROVED
APPROVED WITH CONDITIONS
CHANGES REQUIRED
SPIKE REQUIRED
BLOCKED
```

## Suggested first use

Use a small, concrete database change to calibrate the agent:

- BrewDeck: review the next migration involving `Recipe` or `BrewSession`
- BrickDeck: review a migration for themes, inventories, or import status

After the first review, add repository-specific rules such as migration naming,
supported PostgreSQL version, deployment strategy, expected table sizes, and
whether versioned migrations are considered released after merge or deployment.