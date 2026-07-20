# Generic dependency-upgrade prompts

## Upgrade a vulnerable dependency

```text
@dependency-upgrade-engineer Upgrade [COMPONENT] to remediate [CVE OR ADVISORY].

Identify the declared and resolved versions, dependency-management owner,
minimum fixed version, recommended target, compatibility risks, source and
configuration changes, tests, scanner validation, and rollback implications.

Do not add suppressions merely to pass the scanner.
```

## Full dependency maintenance review

```text
@dependency-upgrade-engineer Review all outdated or vulnerable dependencies.

Separate:
- urgent security upgrades
- supported patch upgrades
- minor maintenance upgrades
- major migrations
- non-actionable scanner noise

Produce a staged plan before changing versions.
```

## Effective dependency-tree investigation

```text
@dependency-upgrade-engineer Investigate why [LIBRARY] resolves to [VERSION].

Trace BOMs, dependency management, plugins, direct declarations, transitive
origins, exclusions, and lockfiles.

Recommend the safest ownership point for the version change.
```

## Major framework migration

```text
@dependency-upgrade-engineer Plan and implement the approved migration from
[CURRENT FRAMEWORK VERSION] to [TARGET VERSION].

Use staged checkpoints and document source, configuration, runtime, test,
container, CI, deployment, rollback, and residual-risk changes.
```

# Spring Boot and Java prompts

## Spring Boot CVE remediation

```text
@dependency-upgrade-engineer Assess and implement the safest Spring Boot upgrade
for the current critical and high CVEs.

Compare targeted overrides, patch upgrade, minor upgrade, and staged major
upgrade.

Verify the effective dependency tree, Spring Security, Hibernate, Jackson,
Flyway, PostgreSQL driver, tests, Docker, and scanner results.
```

## Java 17 to 21

```text
@dependency-upgrade-engineer Upgrade the project from Java 17 to Java 21.

Review toolchains, compiler options, Gradle or Maven, Spring Boot compatibility,
Docker runtime, CI setup, JVM flags, annotation processors, tests, and runtime
agents.

Preserve application behavior.
```

## Gradle wrapper upgrade

```text
@dependency-upgrade-engineer Upgrade Gradle and the Gradle Wrapper to the
approved supported version.

Review plugin compatibility, deprecations, task behavior, test suites,
configuration cache, dependency locking, Java compatibility, and CI.

Do not change unrelated dependencies.
```

## Maven parent and plugin upgrade

```text
@dependency-upgrade-engineer Upgrade the Maven parent, wrapper, and required
plugins.

Verify the effective POM, dependency tree, compiler, Surefire, Failsafe,
dependency management, reproducibility, tests, and scanner results.
```

# Frontend and platform prompts

## Next.js and React upgrade

```text
@dependency-upgrade-engineer Upgrade Next.js, React, TypeScript, and related
tooling to the approved target versions.

Preserve the existing package manager and lockfile format.
Review Node compatibility, routing, server/client behavior, caching, images,
middleware, lint, tests, build output, and peer dependencies.

Coordinate UI fixes with @nextjs-frontend-engineer.
```

## Docker base image upgrade

```text
@dependency-upgrade-engineer Upgrade the backend and frontend base images.

Review runtime compatibility, OS distribution changes, native packages, CA
certificates, non-root behavior, file permissions, health checks, architecture,
image scanning, and SBOM output.

Do not use latest tags.
```

## GitHub Actions upgrade

```text
@dependency-upgrade-engineer Review and upgrade outdated GitHub Actions.

Check action release notes, Node runtime changes, inputs, outputs, permissions,
cache behavior, artifacts, immutable pinning policy, and workflow validation.

Coordinate broader CI changes with @devops-platform-engineer.
```

## Test stack upgrade

```text
@dependency-upgrade-engineer Upgrade JUnit, Mockito, Testcontainers, and other
test tooling required by the framework upgrade.

Preserve meaningful behavior tests and identify changed mock or container
semantics.
```

# BrewDeck and BrickDeck prompts

## BrewDeck full-stack upgrade

```text
@dependency-upgrade-engineer Review and upgrade BrewDeck's Java 21, Spring Boot,
Gradle, PostgreSQL driver, Flyway, Next.js, React, Docker images, and scanners.

Use separate safe stages for backend, frontend, container, and CI changes.
Preserve BrewSession, recipe, ownership, UUID, and feature-flag behavior.
```

## BrickDeck backend upgrade

```text
@dependency-upgrade-engineer Review and upgrade BrickDeck's Java 21, Spring Boot,
Gradle, HTTP client, Jackson, Flyway, PostgreSQL driver, Testcontainers, and
Docker image.

Preserve Rebrickable authentication, error mapping, import idempotency, cache
status, provenance, and user collection data.
```

## Rebrickable client dependency

```text
@dependency-upgrade-engineer Upgrade the HTTP or JSON libraries used by the
BrickDeck Rebrickable integration.

Coordinate timeout, retry, serialization, and contract behavior with
@api-integration-engineer.
```

## Scanner revalidation

```text
@dependency-upgrade-engineer Revalidate the completed upgrade using the
effective dependency tree, OWASP Dependency-Check, container scanning, SBOM, and
relevant test suites.

List all remaining critical and high findings with evidence-based dispositions.
```
