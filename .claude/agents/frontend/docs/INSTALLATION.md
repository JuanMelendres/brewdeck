# Installation

The agent lives at:

```text
.claude/agents/frontend/nextjs-frontend-engineer.md
```

Claude Code scans `.claude/agents/` recursively, so the `frontend/` folder is only
organizational and the `docs/` and `examples/` subfolders are ignored by the agent
scanner. Invoke the agent as `nextjs-frontend-engineer`.

## Install in another project

```bash
mkdir -p <your-project>/.claude/agents/frontend

cp .claude/agents/frontend/nextjs-frontend-engineer.md \
  <your-project>/.claude/agents/frontend/nextjs-frontend-engineer.md
```

Optionally copy the package docs (including `docs/templates/`) and permission example:

```bash
cp -R .claude/agents/frontend/docs <your-project>/.claude/agents/frontend/
cp -R .claude/agents/frontend/examples <your-project>/.claude/agents/frontend/
```

Do not create a second copy of `nextjs-frontend-engineer.md` anywhere under
`.claude/agents/`; a duplicate agent name gets registered twice.

## User-level installation

```bash
mkdir -p ~/.claude/agents/frontend

cp .claude/agents/frontend/nextjs-frontend-engineer.md \
  ~/.claude/agents/frontend/nextjs-frontend-engineer.md
```

Project-level installation is recommended because frontend stacks, scripts, and
directory layouts differ between repositories.

## Verify

Start Claude Code from the repository root:

```bash
claude
```

Invoke:

```text
@nextjs-frontend-engineer Inspect the frontend architecture and report the
framework, routing, styling, state, API, forms, testing, and accessibility
conventions.

Do not modify files.
```

Restart Claude Code if the new agent is not discovered.
