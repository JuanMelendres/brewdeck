# Permissions guide

The `security-auditor` is an independent reviewer and intentionally has no
`Edit` or `Write` tools:

```yaml
tools: Read, Grep, Glob, Bash
permissionMode: default
```

It may request approval to run local scanners or builds, but it cannot directly
change dependencies, source code, Dockerfiles, workflows, tests, or
configuration.

## Optional settings

This package includes:

```text
CONFIGURATION/settings.permissions.example.json
```

and the ready-to-copy equivalent:

```text
INSTALL-IN-PROJECT/.claude/examples/settings.permissions.example.json
```

Review and merge rules into your existing settings. Do not replace project
settings blindly.

## Recommended approvals

After confirming the repository and command:

```text
./gradlew dependencies
./gradlew dependencyInsight ...
./gradlew test
./gradlew check
./mvnw dependency:tree
./mvnw test
./mvnw verify
docker build ...
docker compose config
trivy fs .
trivy image ...
grype ...
syft ...
```

Scanner availability varies by environment.

## Deny or carefully restrict

```text
git push
git commit
git merge
git rebase
git reset --hard
git clean -fd
rm -rf
docker system prune
docker volume rm
kubectl delete
helm uninstall
curl ... | sh
wget ... | sh
flyway clean
flyway repair
```

Also deny reading:

```text
.env
.env.*
*.pem
*.key
secrets/**
credential files
cloud configuration containing tokens
```

## Secret handling

The agent should report:

- file path
- secret category
- whether the value appears active
- a redacted fingerprint when essential

It must never reproduce the full value.

## Scanner suppressions

The agent must not create or recommend a suppression without:

1. Exact advisory identifier
2. Component and version
3. Evidence that the finding is not applicable or reachable
4. Expiration or review date
5. Owner
6. Documented residual risk

Even then, implementation belongs to the engineering team, not this reviewer.

## Handoff pattern

```text
@spring-backend-engineer Implement SEC-001 through SEC-004 from the
security-auditor report. Preserve existing behavior, add tests, and do not add
scanner suppressions.
```

Then:

```text
@security-auditor Re-review SEC-001 through SEC-004. Verify the effective
dependency tree, runtime configuration, tests, and scanner output. Do not modify
files.
```
