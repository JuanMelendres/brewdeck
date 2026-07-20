# Optional Claude Code permission guardrails

The agent instructions define behavioral boundaries, but enforceable controls belong in Claude Code permission rules, sandboxing, and reviewed hooks.

This package includes an example file at:

```text
.claude/agents/quality/examples/settings.permissions.example.json
```

It is intentionally not named `.claude/settings.json`, so extracting the package does not silently change project permissions.

## Recommended setup

1. Open the project's existing `.claude/settings.json`, when present.
2. Review the example rules individually.
3. Merge only rules that match the repository, operating system, and build tool.
4. Start Claude Code and inspect the effective rules with `/permissions`.
5. Test the configuration in a disposable branch or worktree.

## Why the session stays in `default` permission mode

`permissionMode` is not honored in subagent frontmatter, so the agent does not declare it and runs under the session's mode. The agent must create tests and execute test commands, but broad automatic edit or Bash approval would be excessive for the first deployment. Keeping the session in `default` mode keeps write and execution requests visible for review.

After the agent has proven reliable, allow exact project commands rather than unrestricted Bash.

## Suggested allow strategy

For a Gradle project, consider exact or narrow commands such as:

```json
{
  "permissions": {
    "allow": [
      "Bash(./gradlew test *)",
      "Bash(./gradlew check *)",
      "Bash(./gradlew build *)",
      "Bash(./gradlew spotlessCheck *)",
      "Bash(git status --short)",
      "Bash(git diff *)"
    ]
  }
}
```

For Maven, use the equivalent `./mvnw` commands. Do not copy both sets when the repository only uses one build tool.

## Testcontainers and Docker

Testcontainers may need Docker access. Keep this permission under `ask` at first:

```json
{
  "permissions": {
    "ask": [
      "Bash(docker compose *)",
      "Bash(docker-compose *)",
      "Bash(docker ps *)"
    ]
  }
}
```

Deny destructive Docker commands such as system prune, volume removal, or `compose down -v` unless the user explicitly performs them outside the agent.

## Protect production and shared environments

Permissions cannot infer whether a database is production solely from a command string. Combine permission rules with:

- Test-only database URLs
- Disposable Testcontainers databases
- Separate local credentials
- Network isolation
- Read-only shared credentials
- Explicit environment naming
- Human approval before any external connection

Never place production credentials in agent files, prompts, test resources, or Claude settings.

## Deny-list limitations

Shell commands can be composed, aliased, or executed through scripts. A deny list is helpful but not a complete security boundary. Combine:

- `permissions.deny`
- `permissions.ask`
- Claude Code sandboxing
- Reviewed `PreToolUse` hooks
- Disposable branches or worktrees
- Containers and test-only databases
- Normal operating-system account permissions

Hooks execute with the system user's permissions. Do not install unreviewed hook scripts.

## Recommended first-run posture

For the first BrewDeck or BrickDeck run:

- Keep the session in `default` permission mode.
- Approve only focused test edits.
- Approve the exact Gradle or Maven command being executed.
- Reject access to `.env`, secrets, and shared environments.
- Inspect `git diff` before accepting a production-file change.