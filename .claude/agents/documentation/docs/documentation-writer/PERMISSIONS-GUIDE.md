# Permissions guide

The `documentation-writer` can create and edit documentation:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

Unlike review-only agents, it needs `Edit` and `Write`. Keep its scope limited
to documentation. The frontmatter declares only supported subagent fields
(`name`, `description`, `tools`, `model`, `color`); it does not set
`permissionMode`, `maxTurns`, or `effort`, which are not honored in subagent
frontmatter. The agent runs under the session's permission mode (`default` unless
you change it). Constrain its writable paths with the example settings below.

## Recommended writable locations

```text
README.md
CHANGELOG.md
CONTRIBUTING.md
docs/**
adr/**
architecture/**
decisions/**
design/**
product/**
api/**
runbooks/**
testing/**
security/**
```

Repository conventions take precedence.

## Do not edit

```text
src/**
app/**
api/src/**
web/src/**
frontend/src/**
backend/src/**
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
db/migration/**
```

Review the included example rules before merging them into project settings.

## Safe placeholders

Use examples such as:

```text
YOUR_API_KEY
postgresql://USER:PASSWORD@HOST:PORT/DATABASE
https://api.example.com
```

## Handoff

```text
@spring-backend-engineer Resolve DOC-001 by aligning implementation with the
approved behavior. Do not change documentation until validation completes.
```

Then:

```text
@documentation-writer Re-review DOC-001 and update canonical documentation to
match the validated implementation.
```
