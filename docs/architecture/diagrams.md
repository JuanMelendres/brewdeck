# Diagrams

Central place for BrewDeck Mermaid diagrams. Keep them close to the code they describe.

## System context

```mermaid
flowchart LR
  User((User)) --> Web[Next.js Web App]
  Web -->|REST / JSON| API[Spring Boot API]
  API --> DB[(PostgreSQL 16)]
  API -.feature-flagged.-> Claude[Anthropic Claude API]
```

## Request lifecycle (authenticated write)

```mermaid
sequenceDiagram
  participant W as Web App
  participant F as JWT Filter
  participant C as Controller
  participant S as Service
  participant R as Repository
  participant DB as PostgreSQL
  W->>F: POST /api/recipes (Bearer token)
  F->>F: validate JWT
  F->>C: forward request
  C->>C: validate request body
  C->>S: create(recipe)
  S->>R: save(entity)
  R->>DB: INSERT
  DB-->>R: row
  R-->>S: entity
  S-->>C: DTO
  C-->>W: 201 Created + DTO
```

## Auth gate

```mermaid
flowchart TD
  Req[Incoming request] --> Q{Path}
  Q -->|/api/public/** or /api/auth/register,login| Open[Allow]
  Q -->|other /api/**| Check{Valid JWT?}
  Check -->|yes| Allow[Proceed]
  Check -->|no| Deny[401 JSON]
```

> Add feature-specific diagrams here or inline in the relevant spec under `docs/superpowers/specs/`.
