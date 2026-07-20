# Claude Code Security Auditor Agent

Complete reusable security-review package for BrewDeck, BrickDeck, and future
Java/Spring Boot projects.

The agent reviews application security, authorization, dependencies,
containers, CI/CD, configuration, secrets, logging, and supply-chain risk. It
produces remediation guidance but does not modify files.

## Visible contents

```text
AGENT/security-auditor.md
EXAMPLES/ALL-EXAMPLE-PROMPTS.md
EXAMPLES/GENERIC.md
EXAMPLES/BREWDECK.md
EXAMPLES/BRICKDECK.md
EXAMPLES/RELEASE-AND-CI.md
CONFIGURATION/PERMISSIONS-GUIDE.md
CONFIGURATION/settings.permissions.example.json
TEMPLATES/SECURITY-AUDIT-REPORT.md
INSTALL-IN-PROJECT/.claude/agents/quality/security-auditor.md
INSTALL-IN-PROJECT/.claude/examples/settings.permissions.example.json
INSTALLATION.md
MANIFEST.txt
```

## Why there are visible and hidden copies

Claude Code expects project agents inside `.claude/agents/`, but folders whose
names begin with a dot may be hidden by Finder or some ZIP preview tools.

Therefore:

- `AGENT/` contains a visible copy.
- `EXAMPLES/` contains visible prompts.
- `INSTALL-IN-PROJECT/` contains the exact `.claude` structure to copy.

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
```

The security reviewer should evaluate the implementation independently and hand
fixes back to the appropriate engineering agent.

## Typical use

```text
@security-auditor Review the current release changes.

Triage dependency vulnerabilities by exploitability, inspect authentication,
authorization, secrets, Docker, CI/CD, and logging, and provide exact
remediation and validation steps.

Do not modify files.
```

## Main review statuses

```text
APPROVED
APPROVED WITH CONDITIONS
CHANGES REQUIRED
SECURITY SPIKE REQUIRED
BLOCKED
```

## Important limitation

An `APPROVED` result means no blocking findings were identified within the
reviewed scope and available evidence. It does not certify that the application
is fully secure.
