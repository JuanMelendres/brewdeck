# Installation

The agent lives at:

```text
.claude/agents/security/security-auditor.md
```

Claude Code scans `.claude/agents/` recursively, so the `security/` folder is
only organizational and the `docs/` and `examples/` subfolders are ignored by
the agent scanner. Invoke the agent as `security-auditor`.

## Install in another project

```bash
mkdir -p <your-project>/.claude/agents/security

cp .claude/agents/security/security-auditor.md \
  <your-project>/.claude/agents/security/security-auditor.md
```

Optionally copy the package docs and permission example too:

```bash
cp -R .claude/agents/security/docs <your-project>/.claude/agents/security/
cp -R .claude/agents/security/examples <your-project>/.claude/agents/security/
```

Do not create a second copy of `security-auditor.md` anywhere under
`.claude/agents/`; a duplicate agent name gets registered twice.

## User-level installation

```bash
mkdir -p ~/.claude/agents/security

cp .claude/agents/security/security-auditor.md \
  ~/.claude/agents/security/security-auditor.md
```

Project-level installation is recommended so rules and revisions can be
versioned with each repository.

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
