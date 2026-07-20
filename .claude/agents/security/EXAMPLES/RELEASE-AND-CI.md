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
