# Security

Security review outputs for BrewDeck, kept as living docs alongside the code.

## Contents

- `spring-vulnerability-audit.md` — dependency & container vulnerability audit report, generated and
  updated by the `spring-vulnerability-audit` skill. Created on the first audit run.

## How audits run

The audit targets the `brewdeck-api` Maven module (`brewdeck-web` npm dependencies are out of scope).

```bash
# Read-only audit (default): analysis + report, no file edits
/spring-vulnerability-audit audit brewdeck-api

# Plan an upgrade: exact target versions + staged migration, no edits
/spring-vulnerability-audit plan brewdeck-api

# Apply approved safe updates + validation
/spring-vulnerability-audit apply brewdeck-api
```

OWASP Dependency-Check is wired into the `security` Maven profile (`failBuildOnCVSS=7`) and a weekly
[`.github/workflows/security.yml`](../../.github/workflows/security.yml):

```bash
cd brewdeck-api && sh mvnw clean verify -Psecurity   # report → target/dependency-check-report.html
```

> No `NVD_API_KEY` is configured, so the first NVD data download is slow and may be incomplete —
> a partial scan is reported as a limitation, never as "no vulnerabilities."
