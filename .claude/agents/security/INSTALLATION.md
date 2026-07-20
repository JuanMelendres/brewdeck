# Installation

## Project installation

Copy:

```text
INSTALL-IN-PROJECT/.claude/agents/quality/security-auditor.md
```

to:

```text
<your-project>/.claude/agents/quality/security-auditor.md
```

Or copy the complete hidden folder:

```bash
cp -R INSTALL-IN-PROJECT/.claude <your-project>/
```

Review before overwriting any existing files.

## Manual installation

```bash
mkdir -p <your-project>/.claude/agents/quality

cp AGENT/security-auditor.md \
  <your-project>/.claude/agents/quality/security-auditor.md
```

## User-level installation

```bash
mkdir -p ~/.claude/agents/quality

cp AGENT/security-auditor.md \
  ~/.claude/agents/quality/security-auditor.md
```

Project-level installation is recommended so rules and revisions can be
versioned with each repository.

## Show hidden folders on macOS

In Finder:

```text
Command + Shift + .
```

## Verify

Start Claude Code from the repository root:

```bash
claude
```

Then run:

```text
@security-auditor Review the current repository security posture.
Do not modify files.
```

When the agent is not discovered, restart Claude Code after creating
`.claude/agents/`.
