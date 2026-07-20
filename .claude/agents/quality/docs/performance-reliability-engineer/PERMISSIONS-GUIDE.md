# Permissions guide

The agent can create performance tests, benchmarks, profiling configuration,
and focused approved fixes:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

The settings example is conservative and must be adapted to the repository.

## Safe writable areas

Typical paths:

```text
performance/**
benchmarks/**
load-tests/**
tests/performance/**
docs/performance/**
observability/**
monitoring/**
```

Focused application edits should be allowed only for explicitly approved files.

## Protected areas

Baseline rules deny:

```text
.env*
secrets/**
*.pem
*.key
**/db/migration/**
.github/workflows/**
infrastructure deployment files
dependency manifests and lockfiles
```

Coordinate CI changes with `devops-platform-engineer` and schema changes with
`database-migration-reviewer`.

## Commands requiring approval

Examples:

```text
./gradlew test
./gradlew jmh
./mvnw test
./mvnw verify
k6 run ...
gatling ...
npm run build
npm run test
docker compose ...
jcmd ...
jfr ...
```

Profiling and load tests can consume significant resources, so keep permission
prompts enabled.

## Commands to deny

```text
git push
git commit
git merge
git reset
git clean
rm -rf
docker system prune
docker compose down -v
kubectl apply
kubectl delete
helm upgrade
terraform apply
production psql
external-service load tests
```

Never load-test production or third-party APIs without explicit authorization.
