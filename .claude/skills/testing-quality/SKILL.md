---
name: testing-quality
description: Use this skill when adding, updating, debugging, or reviewing tests, coverage, JaCoCo, Spotless, SonarCloud, GitHub Actions, OWASP Dependency Check, or integration tests in BrewDeck.
---

# Testing and Quality Skill

## Role

Act as a senior QA-minded backend engineer with strong experience in JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo, SonarCloud, and CI pipelines.

## Testing Strategy

Use the correct test type:

- Service tests for business logic
- Controller tests for HTTP status, validation, response shape, and error body
- Repository tests for custom query methods
- Specification repository tests for filter behavior
- Integration tests for full workflows

## Paginated API Test Rules

For paginated endpoints, assert:

```java
jsonPath("$.content")
jsonPath("$.content[0].id")
jsonPath("$.page")
jsonPath("$.size")
jsonPath("$.totalElements")
jsonPath("$.totalPages")
```

Never assert:

```java
jsonPath("$[0]")
```

unless the endpoint truly returns a raw array.

## Integration Test Rules

- Use explicit pagination:
    - page=0
    - size=10
    - sort=id,asc
- Do not assume the database contains only one record.
- Use unique names for entities with unique constraints.
- For favorite tests, assert that the target record exists in content, not that content size is exactly 1 unless 
fully isolated.

## Validation Test Rules

For invalid requests, assert:

```java
jsonPath("$.status").value(400)
jsonPath("$.error").value("Bad Request")
jsonPath("$.message").value("Validation failed")
jsonPath("$.validationErrors.fieldName").value("Expected message")
```

## Commands

Focused test (use .cmd suffix if explicitly on Windows CMD):

```bash
./mvnw -Dtest=ClassName test
```

Full verification (use .cmd suffix if explicitly on Windows CMD):

```bash
./mvnw spotless:apply
./mvnw clean verify
```

## Completion Criteria

A task is complete only when:

- Code compiles
- Relevant tests pass
- Full verify is expected to pass
- No obvious Sonar issues are introduced
- A Conventional Commit message is provided