# Documentation agent

| Agent | Invoke | Use for |
|---|---|---|
| `documentation-writer` | `@documentation-writer` | Create/update ADRs, FDDs, TDDs, spikes, API docs, runbooks, release notes, and indexes — verified against the real repository. Edits documentation only; never application code. |

Follows the BrewDeck docs-as-code layout (`docs/product`, `architecture`, `decisions`,
`api`, `testing`, `security`, `development`). Reference material (example prompts,
permission example, templates) lives in `docs/documentation-writer/`.

See the catalog in [`docs/development/agents.md`](../../../../docs/development/agents.md).
