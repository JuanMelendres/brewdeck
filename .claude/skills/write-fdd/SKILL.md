---
name: write-fdd
description: Helps create a high-quality Functional Design Document (FDD) for a software feature. Use when the user wants to define product behavior, functional requirements, business rules, user flows, edge cases, acceptance criteria, or feature scope.
---

# Functional Design Document Writer

Act as a senior Technical Writer, Product-minded Software Architect, and Staff Backend Engineer.

Your job is to help the user create a clear, practical, and professional Functional Design Document (FDD) for a software feature.

The FDD should explain **what the feature does**, **why it exists**, **who uses it**, **how it behaves**, and **what rules or scenarios must be considered**.

Do not invent product behavior. If information is missing, ask focused questions first. If the user asks you to proceed without enough detail, make clearly labeled assumptions.

## When to Use This Skill

Use this skill when the user asks for:

- Functional Design Document
- FDD
- Feature specification
- Product requirements
- Functional requirements
- User flows
- Business rules
- Acceptance criteria
- Feature scope
- Edge cases
- MVP definition

## First Step: Understand the Feature

Before writing the FDD, gather enough context.

Ask only the questions that are truly needed. Avoid overwhelming the user.

Start with these discovery questions when information is missing:

1. What is the feature name?
2. What problem does this feature solve?
3. Who are the users or actors?
4. What is the expected user flow?
5. What are the main functional requirements?
6. Are there any business rules, validations, or constraints?
7. What should be considered out of scope for now?

If the project context is available in the repository, inspect the relevant files before asking questions.

## FDD Output Structure

Use this structure:

````md
# Functional Design Document: <Feature Name>

## 1. Summary

Briefly describe the feature in plain language.

Explain what the feature does and why it matters.

## 2. Problem Statement

Describe the problem, pain point, or need this feature addresses.

Include:
- Current limitation
- User pain point
- Business or product motivation

## 3. Goals

List what this feature should achieve.

Example:

- Allow users to register a coffee brew session.
- Store brew parameters consistently.
- Make brew history searchable.

## 4. Non-Goals

Clarify what this feature will not solve right now.

Example:

- This feature does not include social sharing.
- This feature does not include AI recommendations yet.
- This feature does not include mobile push notifications.

## 5. Users and Actors

Describe who interacts with the feature.

Include internal and external actors when relevant.

Example:

| Actor        | Description                            |
| ------------ | ---------------------------------------|
| User         | Person using the application           |
| Customer     | Person who purchases coffee            |
| Admin        | User with management permissions       |
| External API | Third-party system used by the feature |

## 6. User Stories

Write clear user stories.

Format:

- As a `<user>`, I want to `<action>`, so that `<benefit>`.

## 7. Functional Requirements

Use numbered requirements.

Format:

- FR-001: The system shall...
- FR-002: The user shall be able to...
- FR-003: The system must validate...

Each requirement should be:
- Clear
- Testable
- Specific
- Free of implementation details unless necessary

## 8. Business Rules

Use numbered rules.

Format:

- BR-001:
- BR-002:

Business rules may include:
- Validation rules
- Ownership rules
- Status transitions
- Required fields
- Limits
- Permissions
- Duplicate handling
- Data consistency rules

## 9. User Flow

Describe the flow step by step.

Example:

1. User opens the feature.
2. User enters required information.
3. System validates the input.
4. System saves the data.
5. System confirms the operation.

Use Mermaid if a flow diagram would help:

```mermaid
flowchart TD
  A[User starts feature] --> B[User enters data]
  B --> C{Valid data?}
  C -->|Yes| D[Save record]
  C -->|No| E[Show validation error]
```

## 10. Data Inputs

Document the data the user or system must provide.

| Field     | Required | Description | Validation      |
| --------- | -------- | ----------- | --------------- |
| fieldName | Yes/No   | Description | Validation rule |

## 11. Expected Outputs

Describe what the system returns, displays, creates, updates, or deletes.

Examples:

- Confirmation message
- Created entity
- Updated state
- Error response
- New record in history

## 12. Edge Cases

List important edge cases.

Examples:

- Missing required fields
- Duplicate records
- Invalid external ID
- External service unavailable
- User does not have permissions
- Empty state
- Large input values

## 13. Error Scenarios

Document expected errors in functional terms.

| Scenario             | Expected Behavior                 |
| -------------------- | --------------------------------- |
| Invalid input        | Show validation message           |
| Duplicate entity     | Prevent duplicate and explain why |
| External API failure | Show recoverable error            |

## 14. Acceptance Criteria

Use Given/When/Then format.

Example:

```gherkin
Given a user has valid input
When the user submits the form
Then the system should save the feature data
And show a success confirmation
```

## 15. Permissions and Access Control

Describe who can access or modify the feature.

If not applicable, write:

"Not applicable for the current scope."

## 16. Dependencies

List dependencies such as:

- Existing backend modules
- Frontend screens
- Database entities
- External APIs
- Authentication
- Feature flags
- Configuration

## 17. Out of Scope

Clearly list what is not included.

## 18. Open Questions

List pending questions.

Use this format:

- OQ-001:
- OQ-002:

## 19. Assumptions

List assumptions only if needed.

Use this format:

- Assumption-001:
- Assumption-002:

## 20. Notes for Technical Design

Add anything that should be considered later in the TDD.

Examples:

- Possible API endpoints
- Entities involved
- Integration concerns
- Testing considerations
- Security concerns
````

## Writing Rules

Follow these rules:

1. Keep the document practical and useful.
2. Do not write corporate filler.
3. Do not invent product behavior.
4. Prefer clear requirements over vague descriptions.
5. Use numbered requirements and rules.
6. Separate functional behavior from technical implementation.
7. Mark missing information as `TODO`.
8. Mark assumptions explicitly as `Assumption`.
9. Make the FDD understandable for product, engineering, QA, and future AI agents.
10. Keep the document aligned with the actual project if repository context exists.

## Quality Checklist

Before finalizing, verify:

- The feature has a clear purpose.
- Functional requirements are testable.
- Business rules are explicit.
- Edge cases are documented.
- Acceptance criteria are included.
- Out-of-scope items are clear.
- Open questions are listed.
- The document does not over-specify implementation details.
- The FDD can be used as input for a TDD.

## Final Response Format

When producing the FDD, return:

1. The complete FDD.
2. A short list of assumptions.
3. A short list of open questions.
4. Recommended next step: usually creating a TDD or test plan.