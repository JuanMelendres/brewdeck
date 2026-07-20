# Permissions guide

The `incident-response-engineer` can create incident documentation and local
diagnostic artifacts, but should remain operationally non-destructive:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

## Intended writable locations

```text
incidents/**
postmortems/**
docs/incidents/**
docs/runbooks/**
diagnostics/**
reproduction/**
```

Application-code changes should be handed to implementation agents.

## Secrets and sensitive evidence

Deny reading:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
database dumps
production exports
```

Sanitize logs before sharing them with the agent.

## Safe commands requiring approval

Examples:

```text
git log
git diff
./gradlew test
./mvnw test
npm test
docker compose config
docker compose up <disposable-services>
psql against disposable local databases
```

## Commands to deny

```text
git push
git commit
git merge
git reset
git clean
flyway clean
flyway repair
docker system prune
docker volume rm
docker compose down -v
kubectl apply
kubectl delete
kubectl rollout restart
helm upgrade
terraform apply
production database commands
restart, rollback, deploy, scale, or failover commands
```

The agent proposes operational actions but does not execute them.

## Handoff example

```text
@spring-backend-engineer Implement IR-003 from the incident report and add a
regression test that reproduces the confirmed failure mechanism.
```

Then:

```text
@incident-response-engineer Verify IR-003 against the timeline, failure
mechanism, recovery criteria, and recurrence risk.
```
