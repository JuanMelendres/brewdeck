# Permissions guide

The `api-integration-engineer` can edit integration-specific application code
and tests:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

The included settings are examples and must be adapted to each repository.

## Intended writable areas

Typical patterns:

```text
**/integration/**
**/integrations/**
**/client/**
**/clients/**
**/adapter/**
**/adapters/**
**/external/**
**/gateway/**
**/dto/external/**
**/config/*Client*
**/test/**/integration/**
```

Repository conventions take precedence.

## Protected areas

The baseline example denies edits to:

```text
**/db/migration/**
.github/workflows/**
Dockerfile*
docker-compose*
infra/**
terraform/**
k8s/**
helm/**
frontend source
package manifests and lockfiles
build dependency files
```

It also protects generic domain and controller paths unless the project
configuration is intentionally expanded.

## Secrets

Deny reading:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
```

Use placeholders:

```text
SERVICE_API_KEY
SERVICE_CLIENT_ID
SERVICE_CLIENT_SECRET
https://api.example.com
```

## Dependency changes

Do not automatically edit:

```text
pom.xml
build.gradle
build.gradle.kts
package.json
lockfiles
```

A resilience or HTTP dependency must require explicit approval.

## Recommended command approvals

Examples:

```text
./gradlew test
./gradlew integrationTest
./gradlew check
./mvnw test
./mvnw verify
npm test
docker compose up <mock-or-local-service>
```

Use only repository-defined tasks and disposable local infrastructure.

## Commands to deny

```text
git push
git commit
git merge
git rebase
git reset
git clean
rm -rf
flyway clean
flyway repair
docker system prune
docker compose down -v
kubectl apply
kubectl delete
helm upgrade
terraform apply
vercel deploy
netlify deploy
```

## Handoff example

```text
@database-migration-reviewer Review the proposed provenance and idempotency
schema required by INT-003. Do not modify files.
```

After approval:

```text
@spring-backend-engineer Implement the approved schema and domain changes for
INT-003, then return the integration boundary to @api-integration-engineer.
```
