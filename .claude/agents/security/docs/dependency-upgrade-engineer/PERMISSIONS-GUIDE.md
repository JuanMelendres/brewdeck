# Permissions guide

The `dependency-upgrade-engineer` needs edit access to build files, dependency
manifests, wrappers, compatibility source changes, tests, containers, and
upgrade documentation:

```yaml
tools: Read, Grep, Glob, Edit, Write, Bash
```

The provided settings are examples and must be reviewed for each repository.

## Intended writable files

Depending on scope:

```text
pom.xml
.mvn/**
mvnw
mvnw.cmd
build.gradle
build.gradle.kts
settings.gradle
settings.gradle.kts
gradle/**
gradlew
gradlew.bat
gradle.properties
libs.versions.toml
package.json
package-lock.json
pnpm-lock.yaml
yarn.lock
Dockerfile*
.github/workflows/**
source files required by migration
tests required by migration
docs/upgrades/**
```

Only modify files required by the approved upgrade.

## Protected files

Always protect:

```text
.env
.env.*
secrets/**
*.pem
*.key
credentials*
**/db/migration/**
production configuration containing secrets
```

Released migration files must not be edited.

## Commands requiring approval

Typical commands:

```text
./gradlew dependencies
./gradlew dependencyInsight
./gradlew test
./gradlew check
./gradlew build
./mvnw help:effective-pom
./mvnw dependency:tree
./mvnw test
./mvnw verify
npm install
npm ci
npm run lint
npm run typecheck
npm test
npm run build
trivy
grype
syft
docker build
```

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
docker volume rm
kubectl apply
kubectl delete
helm upgrade
terraform apply
npm publish
mvn deploy
gradle publish
deployment commands
```

## Handoff example

```text
@security-auditor Re-review CVE-001 through CVE-006 after the dependency upgrade.
Verify resolved versions, reachability, scanner results, and residual risk.
```
