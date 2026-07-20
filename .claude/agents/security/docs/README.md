# Claude Code Security Auditor Agent

Complete reusable security-review package for BrewDeck, BrickDeck, and future
Java/Spring Boot projects.

The agent reviews application security, authorization, dependencies,
containers, CI/CD, configuration, secrets, logging, and supply-chain risk. It
produces remediation guidance but does not modify files.

## Package contents

```text
.claude/agents/security/security-auditor.md
.claude/agents/security/docs/README.md
.claude/agents/security/docs/INSTALLATION.md
.claude/agents/security/docs/EXAMPLE-PROMPTS.md
.claude/agents/security/docs/PERMISSIONS-GUIDE.md
.claude/agents/security/docs/SECURITY-AUDIT-REPORT.md
.claude/agents/security/docs/MANIFEST.txt
.claude/agents/security/examples/settings.permissions.example.json
```

## Layout

Claude Code scans `.claude/agents/` recursively for agent definitions. Only
`security-auditor.md` carries agent frontmatter; the `docs/` and `examples/`
subfolders hold human-facing package material and are ignored by the agent
scanner. Invoke the agent as `security-auditor`.

The original distributable duplicated the agent under an `INSTALL-IN-PROJECT/`
skeleton for out-of-tree installation. That copy was removed here: keeping two
`security-auditor.md` files under `.claude/agents/` would register the agent
name twice.

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
