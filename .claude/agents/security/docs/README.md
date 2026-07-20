# Security agents

Security and supply-chain agents.

| Agent | Invoke | Use for |
|---|---|---|
| `security-auditor` | `@security-auditor` | Independent security review: authn/authz, IDOR, input/API security, secrets, Spring Security config, Docker, CI/CD, and dependency risk with exploitability triage (read-only). |
| `dependency-upgrade-engineer` | `@dependency-upgrade-engineer` | Plan and stage dependency/framework upgrades (Spring Boot, Java, npm, base images, Actions) with compatibility analysis, breaking-change notes, and validation. |

`security-auditor` is strictly read-only (no Edit/Write). `dependency-upgrade-engineer`
may edit build files only under an approved, staged plan.

Per-agent reference material lives in `docs/<agent-name>/`. See the catalog in
[`docs/development/agents.md`](../../../../docs/development/agents.md).
