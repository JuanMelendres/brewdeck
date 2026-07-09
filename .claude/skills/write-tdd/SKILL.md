---
name: write-tdd
description: Helps create a high-quality Technical Design Document (TDD) for a software feature, system component, API, database design, integration, backend service, or architecture change.
---

# Technical Design Document Writer

Act as a senior Technical Writer, Software Architect, and Staff Backend Engineer.

Your job is to help the user create a clear, robust, and practical Technical Design Document (TDD).

The TDD should explain **how the solution will be built**, **which components are involved**, **what trade-offs exist**, **how data flows**, **how errors are handled**, and **how the implementation should be tested**.

Do not invent technical details. Inspect the repository when available. If information is missing, ask focused questions or mark assumptions clearly.

## When to Use This Skill

Use this skill when the user asks for:

- Technical Design Document
- TDD
- Technical proposal
- API design
- Database design
- Backend design
- System architecture
- Integration design
- Refactor design
- Technical implementation plan
- Design review preparation

## First Step: Understand the Technical Context

Before writing the TDD, gather enough context.

Ask only the questions that are necessary.

Useful discovery questions:

1. What feature, system area, or technical change is being designed?
2. Is there an existing FDD or requirement document?
3. What parts of the system are involved?
4. What APIs, services, modules, or database tables already exist?
5. Are there external integrations?
6. Are there non-functional requirements such as performance, security, scalability, or observability?
7. Are there constraints such as timeline, existing architecture, or backward compatibility?

If repository context is available, inspect:

- README.md
- CLAUDE.md
- docs/
- Source code
- Tests
- Database migrations
- API controllers
- Services
- Models/entities
- Configuration files
- Docker files
- CI/CD workflows

## TDD Output Structure

Use this structure:

````md
# Technical Design Document: <Feature or System Area>

## 1. Summary

Briefly explain the proposed technical solution.

Include:
- What will be built
- Why it is needed
- Which parts of the system are affected

## 2. Context

Describe the current technical context.

Include:
- Existing behavior
- Existing limitations
- Related modules
- Current architecture
- Relevant constraints

## 3. Goals

List the technical goals.

Example:

- Add a reliable API for creating brew sessions.
- Persist brew session data in PostgreSQL.
- Validate input consistently.
- Add unit and integration tests.

## 4. Non-Goals

Clarify what this design will not address.

Example:

- This design does not include mobile support.
- This design does not include analytics.
- This design does not include authentication changes.

## 5. Requirements Reference

Link or summarize related requirements.

Include references to:
- FDD
- GitHub issue
- Product requirement
- User story
- Existing documentation

If no FDD exists, write:

"TODO: No FDD or functional requirements document exists yet."

## 6. Current Architecture

Describe the current architecture before the change.

Use a diagram if useful:

```mermaid
flowchart LR
  Client --> API
  API --> Service
  Service --> Database
```

## 7. Proposed Architecture

Describe the target architecture after the change.

Include:

- Components
- Responsibilities
- Boundaries
- Data flow
- External dependencies

Use Mermaid when helpful:

```mermaid
flowchart LR
  Client --> Controller
  Controller --> Service
  Service --> Repository
  Repository --> PostgreSQL
```

## 8. Component Design

Break down the design by component.

Example:

### 8.1 Controller Layer

Responsibilities:

- Expose HTTP endpoints
- Validate request shape
- Map requests to service calls
- Return appropriate responses

### 8.2 Service Layer

Responsibilities:

- Enforce business logic
- Coordinate repositories
- Handle external API calls
- Manage transactions

### 8.3 Repository/Data Access Layer

Responsibilities:

- Persist and retrieve data
- Encapsulate query logic
- Avoid leaking database details

### 8.4 External Integration Layer

Responsibilities:

- Communicate with third-party APIs
- Handle retries, failures, and mapping
- Avoid coupling external models directly to internal models

## 9. Data Model

Describe entities, tables, fields, and relationships.

Use this format:

| Entity/Table | Purpose     |
| ------------ | ----------- |
| table_name   | Description |

Then document important fields:

| Field | Type | Required | Notes |
| ----- | ---- | -------- | ----- |

Include:

- Primary keys
- Foreign keys
- Indexes
- Constraints
- Uniqueness rules
- Audit fields
- Migration strategy

## 10. API Design

Document endpoints.

Use this format:

Endpoint: <METHOD> /path

Purpose:

Request:
```json
{
  "example": "value"
}
```

Response:
```json
{
  "example": "value"
}
```

Error responses:

| Status | Reason             |
| ------ | ------------------ |
| 400    | Invalid input      |
| 404    | Resource not found |
| 409    | Conflict           |
| 500    | Unexpected error   |

## 11. Validation Rules

List validation rules.

Format:

- VR-001:
- VR-002:

Examples:

- Required fields
- Allowed values
- Min/max lengths
- Numeric ranges
- Date constraints
- Duplicate prevention
- External ID validation

## 12. Error Handling

Describe how errors will be handled.

Include:

- Validation errors
- Not found errors
- Conflict errors
- External API failures
- Database failures
- Unexpected errors

Use this format:

| Error Type | Handling Strategy | User/System Response |
| ---------- | ----------------- | -------------------- |

## 13. Security Considerations

Document relevant security concerns.

Include:

- Authentication
- Authorization
- Input validation
- Sensitive data
- Secrets management
- External API keys
- Logging safety
- Rate limiting
- Data exposure

If not applicable, explain why.

## 14. Observability

Document how the feature should be observable.

Include:

- Logs
- Metrics
- Tracing
- Correlation IDs
- Audit events
- Error monitoring

If the project does not have observability tooling yet, write lightweight recommendations.

## 15. Performance and Scalability

Document relevant performance considerations.

Include:

- Expected data volume
- Query performance
- Indexes
- Caching
- Pagination
- External API latency
- Batch processing
- Future scaling concerns

## 16. Transaction and Consistency Strategy

Describe:

- Transaction boundaries
- Atomic operations
- Idempotency
- Consistency expectations
- Retry behavior
- Duplicate handling

If not relevant, state why.

## 17. Testing Strategy

Describe how the design should be tested.

Include:

### Unit Tests

- Services
- Validators
- Mappers
- Business rules
- Error handling

### Integration Tests
- API endpoints
- Database persistence
- Repository behavior
- External integration mocks
- Testcontainers if applicable

### Contract/API Tests

- OpenAPI validation
- Postman collection
- Request/response examples

### Edge Case Tests

- Invalid input
- Empty states
- Duplicates
- Missing resources
- External failures

## 18. Rollout Plan

Describe how this change should be introduced.

Include:

- Migration steps
- Backward compatibility
- Feature flags if applicable
- Data migration
- Deployment order
- Rollback strategy

For small personal projects, keep this section lightweight.

## 19. Alternatives Considered

Document alternatives and trade-offs.

Use this format:

| Option | Pros | Cons | Decision |
| ------ | ---- | ---- | -------- |

## 20. Risks and Mitigations

List technical risks.

Use this format:

| Risk | Impact | Mitigation |
| ---- | ------ | ---------- |

## 21. Open Questions

List pending technical questions.

Use:

- OQ-001:
- OQ-002:

## 22. Assumptions

List assumptions.

Use:

- Assumption-001:
- Assumption-002:

## 23. Implementation Plan

Break the work into clear steps.

Example:

1. Create database migration.
2. Add entity/model.
3. Add repository.
4. Add service logic.
5. Add controller endpoint.
6. Add validation.
7. Add unit tests.
8. Add integration tests.
9. Update OpenAPI/Postman.
10. Update documentation.
````

## Writing Rules

Follow these rules:

1. Keep the document technical but understandable.
2. Do not over-engineer the solution.
3. Do not invent architecture that is not needed.
4. Prefer explicit trade-offs.
5. Keep the design aligned with the current codebase.
6. Clearly separate current architecture from proposed architecture.
7. Include testing and error handling.
8. Include security considerations even if the answer is "not applicable yet."
9. Use Mermaid diagrams only when useful.
10. Mark missing details as `TODO`.
11. Mark assumptions explicitly as `Assumption`.
12. Make the TDD useful for implementation, review, and future maintenance.

## Quality Checklist

Before finalizing, verify:

- The technical goal is clear.
- The design is implementable.
- Components and responsibilities are clear.
- API design is documented.
- Data model is documented.
- Error handling is documented.
- Security is considered.
- Testing strategy is specific.
- Alternatives and trade-offs are included.
- Risks are documented.
- Open questions are clear.
- The TDD can guide implementation without needing a long meeting.

## Final Response Format

When producing the TDD, return:

1. The complete TDD.
2. A short summary of key technical decisions.
3. A short list of assumptions.
4. A short list of open questions.
5. A recommended implementation sequence.