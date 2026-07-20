# Generic security prompts

## Current-diff review

```text
@security-auditor Review the current working-tree changes.

Inspect application code, authentication, authorization, validation, dependency
changes, Docker, CI/CD, secrets exposure, logging, and tests.

Triage every finding by exploitability and impact.
Do not modify files.
Do not add suppressions.
Do not commit, push, merge, or deploy.
```

## Full repository audit

```text
@security-auditor Perform a full security audit of this repository.

Cover:
- authentication and authorization
- object-level access control
- API and input security
- dependency and supply-chain risk
- secrets and sensitive data
- Spring Security configuration
- PostgreSQL security
- Docker hardening
- GitHub Actions security
- logging and incident readiness

Run safe local validation only after approval.
Do not modify files.
```

## CVE triage

```text
@security-auditor Triage all current critical and high dependency findings.

For each CVE, identify:
- resolved component and version
- direct or transitive origin
- fixed version
- reachability
- exploit prerequisites
- recommended remediation
- breaking-change risk
- validation steps

Do not suppress findings only to make the scanner pass.
```

## Spring Boot upgrade assessment

```text
@security-auditor Assess whether upgrading Spring Boot is the safest way to
remediate the current vulnerabilities.

Compare:
- targeted dependency overrides
- patch or minor Spring Boot upgrades
- staged major-version upgrade

Provide a step-by-step upgrade sequence with tests, configuration changes,
breaking risks, and rollback checkpoints.

Do not modify files.
```
