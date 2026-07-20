# Permissions guide

The `nextjs-frontend-engineer` needs edit access to frontend source and frontend
tests:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

The frontmatter declares only supported subagent fields (`name`, `description`,
`tools`, `model`, `color`); it does not set `permissionMode`, `maxTurns`, or
`effort`, which are not honored in subagent frontmatter. The agent runs under the
session's permission mode (`default` unless you change it).

Because repository layouts vary, the included settings are examples and must be
reviewed before merging. In BrewDeck the frontend lives in `brewdeck-web/` and the
package manager is pnpm.

## Intended writable areas

Typical locations include:

```text
web/**
frontend/**
ui/**
apps/web/**
packages/ui/**
src/app/**
src/pages/**
src/components/**
src/features/**
src/hooks/**
src/lib/**
src/styles/**
tests/**
e2e/**
```

Only frontend files within these paths should be changed.

## Protected areas

The example configuration denies edits to common backend and infrastructure
locations:

```text
api/src/**
backend/src/**
server/src/**
**/src/main/java/**
**/src/test/java/**
**/db/migration/**
pom.xml
build.gradle*
Dockerfile*
docker-compose*
.github/workflows/**
infra/**
terraform/**
k8s/**
helm/**
```

Adjust these rules to the actual monorepo before use.

## Package dependencies

Do not automatically change:

```text
package.json
package-lock.json
pnpm-lock.yaml
yarn.lock
```

A new dependency should require explicit approval and a clear justification.

## Secrets

Deny reading:

```text
.env
.env.*
*.pem
*.key
secrets/**
credentials*
```

Never place secrets in:

```text
NEXT_PUBLIC_*
client bundles
browser storage
source maps
test snapshots
logs
```

## Recommended command approvals

Use repository scripts where available:

```text
npm run lint
npm run typecheck
npm test
npm run test
npm run test:e2e
npm run build
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

Commands can create build artifacts, so default permission prompts should
remain enabled.

## Destructive or publishing commands

Deny:

```text
git push
git commit
git merge
git rebase
git reset
git clean
npm publish
pnpm publish
yarn publish
vercel deploy
netlify deploy
rm -rf
```

## Backend mismatch handoff

```text
@spring-backend-engineer Resolve FE-API-001 by implementing the approved API
contract. Do not change the frontend contract without architecture approval.
```

After backend validation:

```text
@nextjs-frontend-engineer Complete FE-API-001 using the validated backend
contract and rerun frontend tests.
```
