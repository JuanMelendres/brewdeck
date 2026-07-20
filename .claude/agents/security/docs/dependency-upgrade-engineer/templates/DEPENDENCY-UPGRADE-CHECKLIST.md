# Dependency Upgrade Checklist

## Inventory

- [ ] Runtime versions identified
- [ ] Build-tool versions identified
- [ ] Declared versions identified
- [ ] Resolved versions identified
- [ ] BOM or platform ownership identified
- [ ] Container images identified
- [ ] CI actions identified
- [ ] Scanner findings captured

## Target selection

- [ ] Minimum fixed version identified
- [ ] Supported stable target selected
- [ ] Release notes reviewed
- [ ] Migration guide reviewed
- [ ] Compatibility matrix created
- [ ] Unsupported snapshots avoided

## Implementation

- [ ] Version ownership changed at correct level
- [ ] Source changes focused
- [ ] Configuration migrated
- [ ] Tests updated for real behavior
- [ ] No released migrations modified
- [ ] No broad suppressions added
- [ ] Lockfiles use existing package manager

## Validation

- [ ] Effective dependency graph verified
- [ ] Compile or type-check passes
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Migration tests pass when relevant
- [ ] Frontend build passes
- [ ] Container build passes
- [ ] Dependency scanner rerun
- [ ] Container scanner rerun
- [ ] SBOM regenerated when applicable

## Release readiness

- [ ] Breaking changes documented
- [ ] Rollback understood
- [ ] Reintroduced CVE risk documented
- [ ] Feature flags considered
- [ ] Specialist reviews completed
- [ ] Final diff focused
