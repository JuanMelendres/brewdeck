# Permissions guide

The `pull-request-reviewer` is intentionally read-only:

```yaml
tools: Read, Grep, Glob, Bash
```

It has no `Edit` or `Write` tools.

## Safe activities

- Read source and documentation
- Search the repository
- Inspect Git status and diffs
- Run approved tests, builds, lint, and validation commands
- Produce review findings and agent handoffs

## Protected information

Deny reading:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
```

## Commands requiring approval

Examples:

```text
./gradlew test
./gradlew check
./mvnw test
./mvnw verify
npm run lint
npm run typecheck
npm test
npm run build
docker compose config
```

## Commands to deny

```text
git commit
git push
git merge
git rebase
git reset
git clean
git tag
gh pr merge
gh pr review --approve
rm -rf
flyway clean
flyway repair
docker system prune
docker compose down -v
kubectl apply
kubectl delete
helm upgrade
terraform apply
deployment commands
```

The agent reports a review decision in chat only. It must not approve or merge a
remote pull request.

## Handoff pattern

```text
@spring-backend-engineer Resolve PR-001 and PR-002 from the
pull-request-reviewer report. Add focused tests and do not modify unrelated
files.
```

Then:

```text
@pull-request-reviewer Re-review PR-001 and PR-002 and verify the remediation
and validation evidence.
```
