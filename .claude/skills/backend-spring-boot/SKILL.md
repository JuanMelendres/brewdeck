---
name: backend-spring-boot
description: Use this skill when modifying the BrewDeck Spring Boot backend, including controllers, services, repositories, DTOs, filters, specifications, validation, pagination, Flyway migrations, or API behavior.
---

# Backend Spring Boot Skill

## Role

Act as a senior Java 21 and Spring Boot 3 backend engineer.

## Project Context

This project is BrewDeck, a Spring Boot REST API for coffee brewing workflows.

Core packages:
- `coffee`
- `method`
- `recipe`
- `session`
- `common`

Testing Stack:
- JUnit 5
- Mockito
- MockMvc
- Testcontainers

## Rules

When changing backend code:

1. Inspect existing style first.
2. Keep changes small and focused.
3. Preserve existing architecture.
4. Do not introduce unrelated refactors.
5. Update tests when behavior changes (using the defined Testing Stack).
6. Use `PageResponse<T>` for collection GET endpoints, applying `@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC)` in controllers.
7. Use DTOs, not entities, in controllers.
8. Use Bean Validation in request records.
9. Use `GlobalExceptionHandler` for error responses.
10. Use Conventional Commits.

## API Response Rules

Collection GET endpoints return:

```java
PageResponse<T>
```

Detail GET endpoints return:

```java
TResponse
```

POST returns:

```java
ResponseEntity.created(location).body(response)
```

DELETE returns:

```java
ResponseEntity.noContent().build()
```

## Validation Message Rules

Avoid special characters in validation messages.

Use:

```text
degrees Celsius
```

not:

```text
°C
```

## Before Finishing

Suggest the exact tests to run (adapt to ./mvnw.cmd if strictly on Windows CMD):

```bash
./mvnw spotless:apply
./mvnw -Dtest=RelevantTest test
./mvnw clean verify
```

End with a Conventional Commit suggestion.