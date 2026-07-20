# Operations agents

Delivery, platform, and incident agents.

| Agent | Invoke | Use for |
|---|---|---|
| `devops-platform-engineer` | `@devops-platform-engineer` | Docker, Docker Compose, GitHub Actions, CI/CD pipelines, build/release plumbing, environment configuration. |
| `release-manager` | `@release-manager` | Go-live readiness: release scope, risk, rollout/rollback plans, feature-flag gating, and release-gate sign-off (e.g. limited rollout for AI recipe suggestions). |
| `incident-response-engineer` | `@incident-response-engineer` | Independent incident triage, troubleshooting, and postmortems across backend, frontend, DB, integrations, Docker, and CI/CD. |

Per-agent reference material lives in `docs/<agent-name>/`. See the catalog in
[`docs/development/agents.md`](../../../../docs/development/agents.md).
