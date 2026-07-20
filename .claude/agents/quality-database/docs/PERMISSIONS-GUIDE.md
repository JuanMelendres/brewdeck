# Permissions guide

The `database-migration-reviewer` is intentionally configured without `Edit` or
`Write`. Its primary safety boundary is therefore the tool allowlist:

```yaml
tools: Read, Grep, Glob, Bash
```

It can inspect files and request permission for shell-based validation, but it
cannot directly change source files. `permissionMode` is not set in the agent
frontmatter (it is not honored there); the agent runs under the session's mode.

## Why keep the session in `default` permission mode

Database validation commands can create build artifacts, start disposable
containers, or connect to a database. They should not be silently approved.

Default mode lets Claude Code apply normal permission checks and ask for
approval when needed.

## Optional project settings

The package contains:

```text
.claude/agents/quality-database/examples/settings.permissions.example.json
```

This file is an example only. Do not rename it over an existing
`.claude/settings.json` without reviewing and merging the rules.

The example:

- blocks secrets and private keys
- blocks source edits by this workflow at the project settings layer
- blocks destructive Git operations
- blocks Flyway clean and repair commands
- blocks destructive SQL patterns
- asks before Gradle, Maven, Docker, Flyway, or PostgreSQL commands

## Database identity rule

Before approving any command that can connect to PostgreSQL, confirm all of the
following:

1. The target is local and disposable.
2. The connection does not use production or shared staging credentials.
3. The command cannot delete or alter valuable data.
4. Testcontainers or a dedicated local database is preferred.
5. The agent reports the target identity in its final evidence.

Environment names such as `dev` or `test` are not sufficient proof that a
database is disposable.

## Recommended approvals

Usually reasonable after review:

```text
./gradlew test
./gradlew integrationTest
./gradlew flywayValidate
./mvnw test
./mvnw verify
docker compose up <local-db-service>
docker compose ps
git status --short
git diff --stat
```

Exact tasks depend on the repository.

## Commands requiring special caution

```text
./gradlew flywayMigrate
./mvnw flyway:migrate
flyway migrate
psql ...
docker compose down
```

Approve migration execution only against an explicitly identified disposable
local database.

## Commands to deny

```text
flyway clean
flyway repair
git reset --hard
git clean -fd
git push
docker compose down -v
docker volume rm
DROP DATABASE
DROP SCHEMA
DROP TABLE
TRUNCATE
unbounded DELETE
```

Even when `repair` is technically possible, it should not be the default
response to an unexpected checksum. First determine whether migration history
was edited and whether environments have diverged.

## Source-file safety

Because this agent is a reviewer, it should not implement its own findings.

Use this handoff:

```text
@spring-backend-engineer Implement findings DB-001 through DB-004 from the
database-migration-reviewer report using new forward migrations. Do not edit
released migrations.
```

Then request a second review:

```text
@database-migration-reviewer Re-review the remediation for DB-001 through
DB-004. Verify the diff and validation evidence. Do not modify files.
```

## Stronger sandboxing

For higher-risk repositories, enable Claude Code's sandbox and restrict:

- filesystem writes
- network access
- Docker socket access
- credential paths
- additional directories

Keep repository settings under version control only after team review.