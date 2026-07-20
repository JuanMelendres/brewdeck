# Permissions guide

The `release-manager` creates release documentation and inspects evidence:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

## Intended writable locations

Typical paths:

```text
releases/**
docs/releases/**
docs/deployment/**
docs/runbooks/**
changelog/**
release-notes/**
```

## Protected areas

Deny edits to:

```text
**/src/**
**/db/migration/**
pom.xml
build.gradle*
package.json
lockfiles
Dockerfile*
docker-compose*
.github/workflows/**
infra/**
terraform/**
k8s/**
helm/**
```

The agent coordinates releases but does not change implementation or deployment
assets.

## Sensitive information

Deny reading:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
production exports
customer data
```

## Safe commands requiring approval

Examples:

```text
git status --short
git diff --stat
git log --oneline
git tag --list
git show
docker image inspect <local-image>
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
git tag <new-tag>
gh release create
npm publish
mvn deploy
gradle publish
docker push
kubectl apply
kubectl delete
helm upgrade
terraform apply
vercel deploy
netlify deploy
flyway migrate
feature-flag changes
```

## Handoff example

```text
@devops-platform-engineer Resolve REL-PLAT-001 by validating the immutable
artifact, deployment sequence, health checks, smoke tests, monitoring, and
rollback procedure. Do not deploy.
```
