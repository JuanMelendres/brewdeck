# Installation

The agent lives at:

```text
.claude/agents/documentation/documentation-writer.md
```

Claude Code scans `.claude/agents/` recursively, so the `documentation/` folder is
only organizational and the `docs/` and `examples/` subfolders are ignored by the
agent scanner. Invoke the agent as `documentation-writer`.

## Install in another project

```bash
mkdir -p <your-project>/.claude/agents/documentation
cp .claude/agents/documentation/documentation-writer.md \
  <your-project>/.claude/agents/documentation/documentation-writer.md
```

Optionally copy the package docs (including `docs/templates/`) and permission example:

```bash
cp -R .claude/agents/documentation/docs <your-project>/.claude/agents/documentation/
cp -R .claude/agents/documentation/examples <your-project>/.claude/agents/documentation/
```

Do not create a second copy of `documentation-writer.md` anywhere under
`.claude/agents/`; a duplicate agent name gets registered twice.

## User-level installation

```bash
mkdir -p ~/.claude/agents/documentation
cp .claude/agents/documentation/documentation-writer.md \
  ~/.claude/agents/documentation/documentation-writer.md
```

## Verify

Start Claude Code in the repository and run:

```text
@documentation-writer Audit the repository documentation and report canonical
structure, missing documents, stale content, and source mismatches.
```

Restart Claude Code if the agent is not detected after creating `.claude/agents`.
