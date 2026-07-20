# CI/CD Review Checklist

## Build and validation

- [ ] Runtime versions pinned
- [ ] Repository scripts used
- [ ] Cold build succeeds
- [ ] Formatting, lint, type-check, tests, and build included
- [ ] Migration validation included
- [ ] Dependency and secret scanning included
- [ ] Caches are optional for correctness

## Security

- [ ] Minimal token permissions
- [ ] Actions pinned appropriately
- [ ] Fork secrets protected
- [ ] Untrusted input quoted
- [ ] Cache poisoning considered
- [ ] No secrets printed

## Containers

- [ ] Supported base image
- [ ] Multi-stage build
- [ ] Non-root runtime
- [ ] Health check
- [ ] No secrets in layers
- [ ] Image scan and SBOM
- [ ] No floating production tag

## Release and recovery

- [ ] Immutable artifact identity
- [ ] Approval boundary
- [ ] Environment protection
- [ ] Migration ownership
- [ ] Health and smoke verification
- [ ] Rollback or roll-forward
- [ ] Audit trail

## Diff safety

- [ ] Only platform files changed
- [ ] No application or migration changes
- [ ] No gates disabled
- [ ] Pre-existing changes preserved
