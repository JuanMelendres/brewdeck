# Engineering agents

Implementation agents. Each may edit code within its domain and must not silently
change public contracts, security, migrations, or infrastructure. They follow an
approved plan from the planning agents.

| Agent | Invoke | Use for |
|---|---|---|
| `spring-backend-engineer` | `@spring-backend-engineer` | Implement approved Spring Boot backend work: REST endpoints, DTOs, services, JPA, PostgreSQL constraints, new Flyway migrations, tests. |
| `nextjs-frontend-engineer` | `@nextjs-frontend-engineer` | Implement approved frontend behavior in `brewdeck-web/`: pages, components, forms, data fetching, all UI states, accessibility, tests. |
| `api-integration-engineer` | `@api-integration-engineer` | Third-party/internal API integration: resilient HTTP clients, auth, pagination, retries, idempotency, provider isolation, contract tests. |
| `ai-llm-engineer` | `@ai-llm-engineer` | AI/LLM features: RAG, tool calling, prompt/model versioning, structured output, evaluation, safety (e.g. the `ai_recipe_assistant` feature). |
| `ux-accessibility-designer` | `@ux-accessibility-designer` | UX, interaction design, information architecture, and WCAG accessibility specs — after requirements, before or alongside frontend implementation. |

BrewDeck stack: Java 21 / Spring Boot 3 / PostgreSQL (BIGINT identity keys) backend;
Next.js App Router + React 19 + TypeScript + MUI + TanStack Query + RHF/Zod frontend.

Per-agent reference material lives in `docs/<agent-name>/`. See the catalog in
[`docs/development/agents.md`](../../../../docs/development/agents.md).
