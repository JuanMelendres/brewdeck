# Quality agents

Independent reviewers and quality gates. They verify work, add or improve tests,
and hand fixes back to engineering — they do not redesign the product.

| Agent | Invoke | Use for |
|---|---|---|
| `test-quality-engineer` | `@test-quality-engineer` | Independent test/quality review: build a risk model, add high-value unit/controller/repository/integration tests, run validation, report evidence. |
| `database-migration-reviewer` | `@database-migration-reviewer` | Review DB and Flyway changes for existing-data compatibility, ORM alignment, locking, deployment ordering, backfills, and rollback (read-only). |
| `performance-reliability-engineer` | `@performance-reliability-engineer` | Performance and reliability review: N+1 queries, latency, throughput, resource use, failure modes, load-bearing paths. |
| `pull-request-reviewer` | `@pull-request-reviewer` | Final PR review across correctness, tests, security, performance, and docs before merge. |

`database-migration-reviewer` is strictly read-only (no Edit/Write).

Per-agent reference material lives in `docs/<agent-name>/`. See the catalog in
[`docs/development/agents.md`](../../../../docs/development/agents.md).
