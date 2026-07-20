# Claude Code Documentation Writer Agent

Complete package for BrewDeck, BrickDeck, and future software projects.

The agent creates and updates ADRs, FDDs, TDDs, engineering spikes, API docs,
runbooks, release notes, changelogs, READMEs, and documentation indexes while
verifying content against repository evidence.

## Package contents

```text
.claude/agents/documentation/documentation-writer.md
.claude/agents/documentation/docs/README.md
.claude/agents/documentation/docs/INSTALLATION.md
.claude/agents/documentation/docs/EXAMPLE-PROMPTS.md
.claude/agents/documentation/docs/PERMISSIONS-GUIDE.md
.claude/agents/documentation/docs/MANIFEST.txt
.claude/agents/documentation/docs/templates/
├── ADR.md
├── FDD.md
├── TDD.md
├── SPIKE.md
├── API-DOCUMENTATION.md
├── RUNBOOK.md
└── DOCUMENTATION-REVIEW-CHECKLIST.md
.claude/agents/documentation/examples/settings.permissions.example.json
```

Claude Code scans `.claude/agents/` recursively for agent definitions. Only
`documentation-writer.md` carries agent frontmatter; the `docs/` and `examples/`
subfolders are ignored by the agent scanner. Invoke the agent as
`documentation-writer`.

## Recommended workflow

```text
solution-architect
        ↓
spring-backend-engineer
        ↓
test-quality-engineer
        ↓
database-migration-reviewer
        ↓
security-auditor
        ↓
documentation-writer
```

## Typical invocation

```text
@documentation-writer Review the current implementation and update all affected
canonical documentation. Do not modify source code, migrations, dependencies,
or infrastructure.
```
