# [Feature] — Technical Design Document

## Document status

- **Status:** Draft
- **Owner:** TBD
- **Last updated:** YYYY-MM-DD

## Executive summary

## Context

## Goals

## Non-goals

## Existing system

## Proposed architecture

```mermaid
flowchart LR
    Client --> API
    API --> Database
```

## Components and responsibilities

## Data model

## API contracts

## Main flows

```mermaid
sequenceDiagram
    participant C as Client
    participant A as API
    participant D as Database
    C->>A: Request
    A->>D: Operation
    D-->>A: Result
    A-->>C: Response
```

## Security

## Reliability and failure handling

## Observability

## Performance and capacity

## Migration and compatibility

## Testing strategy

## Deployment

## Rollback and recovery

## Alternatives considered

## Risks and mitigations

## Delivery plan

## Open questions

## References
