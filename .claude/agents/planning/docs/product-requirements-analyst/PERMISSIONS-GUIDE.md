# Permissions guide

The `product-requirements-analyst` can create and update product documentation:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

## Intended writable locations

Typical paths:

```text
docs/product/**
docs/requirements/**
docs/fdd/**
product/**
requirements/**
backlog/**
decisions/product/**
```

## Protected areas

The baseline settings deny edits to:

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

The agent defines behavior but does not implement it.

## Secrets and sensitive data

Deny reading:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
customer exports
production data
```

Use safe placeholders and aggregated examples.

## Safe commands

The agent normally needs only:

```text
git status --short
git diff --stat
git diff -- docs/
find docs -type f
```

Build and test commands are usually unnecessary.

## Commands to deny

```text
git commit
git push
git merge
git rebase
git reset
git clean
rm -rf
deployment commands
database mutation commands
package publication
```

## Handoff example

```text
@solution-architect Design the architecture for PRD-001 using the approved goals,
non-goals, functional requirements, business rules, permissions, acceptance
criteria, MVP, dependencies, and open decisions.

Do not change product scope without reporting it.
```
