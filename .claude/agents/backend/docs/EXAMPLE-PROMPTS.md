# Example prompts for `spring-backend-engineer`

## Implement an approved architecture plan

```text
@spring-backend-engineer Implement the approved backend and database portions of the solution-architect plan below.

[PASTE OR REFERENCE THE APPROVED PLAN]

Follow existing repository conventions. Add relevant automated tests, run the narrowest useful validation followed by the module verification, and report exact command outcomes. Do not commit, push, merge, or release.
```

## Small Spring Boot feature

```text
@spring-backend-engineer Implement [FEATURE] in the backend.

Requirements:
- [BEHAVIOR 1]
- [BEHAVIOR 2]
- [VALIDATION]
- [ACCEPTANCE CRITERIA]

Inspect the closest existing feature and follow its package, API, error, persistence, and testing conventions. Keep the change scoped. Do not commit or push.
```

## BrewDeck: BrewMethod CRUD

```text
@spring-backend-engineer Implement the approved BrewMethod CRUD backend for BrewDeck.

Use the existing Coffee feature as the primary convention reference. Preserve UUID identifiers, Java 21, Spring Boot, PostgreSQL, Flyway, validation, and the current error-response format. Add controller, service, repository, persistence, and integration tests appropriate to the repository. Do not implement frontend work. Do not commit or push.
```

## BrewDeck: BrewSession slice

```text
@spring-backend-engineer Implement only the create-and-read backend slice of the approved BrewSession design.

Include the new Flyway migration, JPA mappings, repository, application service, request and response contracts, validation, global error handling integration, and behavioral tests. Do not implement recommendations, analytics, or frontend screens. Preserve existing Coffee, BrewMethod, and Recipe contracts. Do not commit or push.
```

## BrickDeck: Rebrickable pagination

```text
@spring-backend-engineer Implement the approved pagination support for the BrickDeck Rebrickable integration.

Preserve provider isolation, local cache behavior, provenance, idempotency, and existing API contracts. Handle non-success responses and malformed pagination metadata. Add deterministic client and service tests without calling the live provider. Do not alter unrelated imports. Do not commit or push.
```

## BrickDeck: data import idempotency

```text
@spring-backend-engineer Fix duplicate writes in the LEGO set import workflow.

First trace the current controller-to-database flow and identify the natural or external key already used by the repository. Add a regression test that fails before the fix. Implement the smallest safe change, preserve existing cache statuses, and validate repeated imports. Do not change the public response unless required and explicitly reported. Do not commit or push.
```

## Bug fix

```text
@spring-backend-engineer Investigate and fix [BUG].

Reproduce or characterize the failure from existing code and tests. Add a focused regression test, implement the smallest correction, run relevant validation, and report whether any public contract or migration is affected. Avoid unrelated refactoring. Do not commit or push.
```

## Database-backed change

```text
@spring-backend-engineer Implement [DATABASE FEATURE] using a new Flyway migration.

Inspect existing migration naming, identifier, timestamp, constraint, and index conventions. Do not edit prior migrations. Keep the change backward-compatible where possible, ensure JPA mappings match the schema, and add persistence or migration validation supported by the repository. Do not run against production or shared environments. Do not commit or push.
```

## External integration

```text
@spring-backend-engineer Implement the approved [PROVIDER] integration.

Use isolated provider DTOs and mappings. Handle authentication configuration without reading or exposing secret values. Define timeout, pagination, retry, rate-limit, idempotency, error mapping, and observability behavior according to the approved plan. Use deterministic mocks or stubs in tests; do not call the live provider unless explicitly authorized. Do not commit or push.
```

## Review and finish an incomplete backend branch

```text
@spring-backend-engineer Inspect the current working tree and finish only the incomplete backend portion of [FEATURE].

Preserve unrelated user changes. Identify what is already implemented, compare it to the acceptance criteria, complete missing behavior, fix failures caused by the branch, and report pre-existing failures separately. Do not revert unrelated files. Do not commit or push.
```
