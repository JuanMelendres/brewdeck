# Optional Claude Code permission guardrails

The agent prompt defines behavioral limits, but enforceable permission rules belong in Claude Code settings. This package includes an example file at:

```text
.claude/examples/settings.permissions.example.json
```

It is deliberately not named `.claude/settings.json`, so installing the ZIP does not silently change repository permissions.

## Recommended use

1. Open your existing `.claude/settings.json`, if present.
2. Review the example rules one by one.
3. Merge only rules appropriate for your repository and operating system.
4. Start Claude Code and inspect the effective configuration with `/permissions`.
5. Test the rules in a disposable branch or worktree.

## Why permission mode is not set in the agent file

`permissionMode` is not honored in subagent frontmatter, so the agent does not declare it. The agent runs under the session's permission mode. Keep the session in `default` mode for a safer first deployment: `acceptEdits` can automatically approve file edits and common filesystem commands, whereas `default` requires approval before the first file write or non-read-only Bash command.

After the agent proves reliable in your workflow, selectively allow exact build and test commands via `settings.json` rather than allowing unrestricted Bash.

## Example allow-list strategy

Prefer exact commands or narrow prefixes used by the repository, such as:

```json
{
  "permissions": {
    "allow": [
      "Bash(./gradlew test)",
      "Bash(./gradlew check)",
      "Bash(./mvnw test)",
      "Bash(./mvnw verify)",
      "Bash(git status --short)",
      "Bash(git diff *)"
    ]
  }
}
```

Do not copy commands for build tools your project does not use.

## Deny-list limitations

Permission rules are valuable, but shell commands can be composed in many ways. For stronger protection, combine:

- `permissions.deny`
- `permissions.ask`
- Claude Code sandboxing
- reviewed `PreToolUse` hooks
- disposable branches, worktrees, containers, or development databases

Do not use unreviewed hook scripts. Hooks run with the permissions of your system user.
