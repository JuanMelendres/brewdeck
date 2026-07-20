# Permissions guide

The agent can edit platform and operational files:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

## Intended writable areas

```text
.github/workflows/**
Dockerfile*
docker-compose*.yml
compose*.yml
scripts/ci/**
scripts/dev/**
scripts/release/**
infra/**
observability/**
monitoring/**
deploy/**
docs/runbooks/**
docs/platform/**
```

## Protected areas

```text
**/src/main/java/**
**/src/test/java/**
web/src/**
frontend/src/**
app/**
**/db/migration/**
.env*
secrets/**
*.pem
*.key
```

## Commands requiring approval

```text
./gradlew *
./mvnw *
npm run *
pnpm *
docker build *
docker compose *
trivy *
grype *
syft *
actionlint *
yamllint *
kubectl diff *
helm template *
terraform validate
terraform plan
```

## Commands denied by the baseline

```text
git push
git commit
git merge
git reset
git clean
docker system prune
docker volume rm
docker compose down -v
kubectl apply/delete
helm install/upgrade/uninstall
terraform apply/destroy
vercel deploy
netlify deploy
package publishing
flyway clean
flyway repair
```

Review and adapt the example settings before merging them into a repository.
