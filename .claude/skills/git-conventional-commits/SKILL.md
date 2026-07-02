---
name: git-conventional-commits
description: Use this skill when generating commit messages, reviewing staged changes, preparing PR summaries, or deciding how to group commits for BrewDeck.
---

# Git Conventional Commits Skill

## Commit Format

Use:

```text
type(scope): description
```

Examples:

```text
feat(api): add dashboard summary endpoint
fix(api): correct favorites integration test
test(api): add validation coverage for brew sessions
refactor(api): simplify recipe service helpers
docs(api): update backend roadmap
ci(api): update sonar workflow
chore(api): clean unused imports
```

## Allowed Types

- feat
- fix
- test
- refactor
- docs
- ci
- chore
- perf
- style

## Scope

Prefer these scopes:

- api
- db
- ci
- docs
- test
- frontend
- config

## Rules

- Use imperative mood in the description (e.g., "add", not "added" or "adds"). 
- Keep the subject concise (under 50 characters if possible). 
- Do not end the description with a period. 
- For breaking changes, append a! after the type/scope (e.g., feat!(api): ...) and optionally include a BREAKING CHANGE: footer. 
- If multiple unrelated changes exist, suggest splitting them into separate commits.

## Output

When asked for a commit, return explicitly scoped add commands to avoid staging unintended files:

```bash
git add <specific-file-or-folder-1> <specific-file-or-folder-2>
git commit -m "type(scope): message"
```

If changes should be split, provide multiple commit commands.