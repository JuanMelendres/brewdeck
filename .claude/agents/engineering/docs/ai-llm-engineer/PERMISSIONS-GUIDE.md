# Permissions guide

The `ai-llm-engineer` can edit AI-specific source, prompts, schemas,
evaluations, tests, feature configuration, and documentation:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

The provided settings are examples and must be adapted to the repository.

## Intended writable areas

Typical paths:

```text
ai/**
llm/**
prompts/**
evaluations/**
evals/**
rag/**
agents/**
tools/ai/**
**/ai/**
**/llm/**
tests/ai/**
docs/ai/**
```

Application integration files should be explicitly approved.

## Protected areas

Protect:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
**/db/migration/**
production data
customer exports
provider logs containing sensitive content
```

## External provider calls

External model-provider calls should require explicit approval.

CI should use:

- mock providers
- recorded sanitized fixtures
- deterministic test adapters

Do not make paid provider calls mandatory for normal builds.

## Dependencies

Do not automatically edit dependency manifests or lockfiles without approval.

## Commands requiring approval

Examples:

```text
./gradlew test
./mvnw test
npm test
npm run build
python -m pytest
python -m evaluation_runner
docker compose up <local-ai-dependencies>
```

## Commands to deny

```text
git push
git commit
git merge
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
provider deployment commands
production model calls
package publication
```

## Handoff example

```text
@security-auditor Review AI-SEC-001 through AI-SEC-008 for prompt injection,
tool authorization, user-data isolation, provider privacy, logging, and abuse.
```
