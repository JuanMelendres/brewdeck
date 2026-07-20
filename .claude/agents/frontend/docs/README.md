# Claude Code Next.js Frontend Engineer Agent

Complete frontend implementation package for BrewDeck, BrickDeck, and future
Next.js/React projects.

The agent implements approved frontend behavior, API integration, forms,
responsive UI, accessibility, tests, error handling, and performance-conscious
components. It is restricted from modifying backend code, migrations, secrets,
and infrastructure.

## Package contents

```text
.claude/agents/frontend/nextjs-frontend-engineer.md
.claude/agents/frontend/docs/README.md
.claude/agents/frontend/docs/INSTALLATION.md
.claude/agents/frontend/docs/EXAMPLE-PROMPTS.md
.claude/agents/frontend/docs/PERMISSIONS-GUIDE.md
.claude/agents/frontend/docs/MANIFEST.txt
.claude/agents/frontend/docs/templates/
├── FRONTEND-IMPLEMENTATION-REPORT.md
└── FRONTEND-ACCEPTANCE-CHECKLIST.md
.claude/agents/frontend/examples/settings.permissions.example.json
```

Claude Code scans `.claude/agents/` recursively for agent definitions. Only
`nextjs-frontend-engineer.md` carries agent frontmatter; the `docs/` and
`examples/` subfolders are ignored by the agent scanner. Invoke the agent as
`nextjs-frontend-engineer`.

## Recommended workflow

```text
solution-architect
        ↓
spring-backend-engineer
        ↓
nextjs-frontend-engineer
        ↓
test-quality-engineer
        ↓
database-migration-reviewer
        ↓
security-auditor
        ↓
documentation-writer
```

The frontend agent can work after the backend contract exists, or in parallel
when the architecture defines a stable contract.

## Typical invocation

```text
@nextjs-frontend-engineer Implement the frontend portion of the approved feature.

Follow existing Next.js, React, TypeScript, styling, data-fetching, form, and
testing conventions.

Cover loading, empty, success, validation, errors, permissions, accessibility,
responsive behavior, and tests.

Do not modify backend code, migrations, secrets, infrastructure, or public API
contracts.
```

## Completion statuses

```text
IMPLEMENTATION COMPLETE
IMPLEMENTATION COMPLETE WITH LIMITATIONS
CHANGES REQUIRED
BACKEND OR DESIGN BLOCKER
BLOCKED
```
