# Environment Configuration Matrix

| Variable | Purpose | Local | CI | Preview | Staging | Production | Secret | Source | Validation |
|---|---|---|---|---|---|---|---:|---|---|
| EXAMPLE_SETTING | Description | safe-local-value | CI source | preview source | staging source | production source | No | configuration | startup validation |

## Rules

- Never store real secret values in this document.
- Separate frontend-public and backend-private variables.
- Document safe defaults and missing-value behavior.
- Identify the source and owner when known.
