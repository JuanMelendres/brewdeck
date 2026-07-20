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

# BrewDeck security prompts

## BrewDeck authorization review

```text
@security-auditor Review BrewDeck's authorization model.

Trace access to coffees, brew methods, recipes, and brew sessions.
Verify that one authenticated user cannot read, update, delete, or reference
another user's resources by changing the sequential BIGINT identifiers.

Inspect controllers, services, repositories, queries, and tests.
Do not modify files.
```

## BrewDeck API security

```text
@security-auditor Review the BrewDeck API security configuration.

Evaluate:
- Spring Security endpoint matchers
- authentication requirements
- object ownership
- CORS for the Next.js frontend
- CSRF decisions
- validation and request limits
- error responses
- Actuator exposure
- sensitive logging
- feature-flagged incomplete endpoints

Do not modify files.
```

## Future AI Barista threat review

```text
@security-auditor Threat-model the future BrewDeck AI Barista feature.

Cover:
- prompt injection
- tool authorization
- access to user recipes and sessions
- cross-user data leakage
- untrusted tasting notes
- external coffee data
- logging and retention
- rate limits
- human confirmation before write actions

Produce architecture and test handoffs.
Do not modify files.
```

# BrickDeck security prompts

## Rebrickable integration review

```text
@security-auditor Review the BrickDeck Rebrickable integration.

Evaluate:
- API key handling
- outbound request construction
- timeouts and retries
- SSRF risk
- remote image URLs
- malformed or oversized responses
- rate limiting
- import endpoint abuse
- sensitive error leakage
- dependency vulnerabilities

Do not modify files.
```

## BrickDeck import authorization

```text
@security-auditor Review authorization and abuse cases for BrickDeck imports.

Verify:
- who may trigger imports
- per-user and global rate limits
- duplicate or repeated imports
- large pagination requests
- background job permissions
- user collection ownership
- administrative operations
- denial-of-service risk

Do not modify files.
```

## Future marketplace integration

```text
@security-auditor Threat-model a future BrickDeck marketplace and price
comparison integration.

Cover untrusted sellers, remote URLs, scraping, API keys, redirects, SSRF,
content injection, price manipulation, rate limits, provenance, and user privacy.

Produce security requirements before implementation.
```

# Release and CI prompts

## Pre-release security gate

```text
@security-auditor Perform the final security review for the release candidate.

Review the release diff, effective dependency tree, container image, CI/CD,
security configuration, secrets handling, and unresolved scanner findings.

List blockers, conditional approvals, validation evidence, and residual risk.
Do not modify files or deploy.
```

## Docker image review

```text
@security-auditor Review the Dockerfile, Compose configuration, and built image.

Evaluate:
- base image support and vulnerabilities
- root user
- packages and shells
- secrets in layers
- exposed ports
- writable paths
- capabilities
- health checks
- image size and attack surface
- runtime environment variables

Run local image scanning only after approval.
```

## GitHub Actions review

```text
@security-auditor Review all GitHub Actions workflows.

Inspect:
- action pinning
- token permissions
- pull_request_target
- untrusted interpolation
- secret exposure
- artifact integrity
- cache poisoning
- branch protections
- release permissions
- dependency and image scanning

Do not modify workflow files.
```
