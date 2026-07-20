# Permissions guide

The `ux-accessibility-designer` creates and edits design documentation only:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

## Intended writable locations

Typical paths:

```text
docs/ux/**
docs/design/**
docs/accessibility/**
design/**
ux/**
wireframes/**
content/**
```

## Protected areas

Deny edits to:

```text
**/src/**
app/**
api/**
web/**
frontend/**
backend/**
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

The agent specifies frontend behavior but does not implement it.

## Sensitive information

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

Use synthetic examples in design artifacts.

## Safe commands

Usually only read-oriented commands are needed:

```text
git status --short
git diff --stat
git diff -- docs/
find docs -type f
```

## Commands to deny

```text
git commit
git push
git merge
git rebase
git reset
git clean
rm -rf
database mutation
deployment
package publication
```

## Handoff example

```text
@nextjs-frontend-engineer Implement UX-001 using the approved screen
specification, component-state matrix, form behavior, responsive rules,
accessibility requirements, content, and acceptance criteria.

Do not change the product scope or API contract.
```
