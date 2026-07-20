# BrickDeck security prompts

## Rebrickable integration review

```text
@security-auditor Review the BrickDeck Rebrickable integration.

Evaluate:
- API key handling
- outbound request construction
- timeouts and retries
- SSRF risk
- remote image URLs
- malformed or oversized responses
- rate limiting
- import endpoint abuse
- sensitive error leakage
- dependency vulnerabilities

Do not modify files.
```

## BrickDeck import authorization

```text
@security-auditor Review authorization and abuse cases for BrickDeck imports.

Verify:
- who may trigger imports
- per-user and global rate limits
- duplicate or repeated imports
- large pagination requests
- background job permissions
- user collection ownership
- administrative operations
- denial-of-service risk

Do not modify files.
```

## Future marketplace integration

```text
@security-auditor Threat-model a future BrickDeck marketplace and price
comparison integration.

Cover untrusted sellers, remote URLs, scraping, API keys, redirects, SSRF,
content injection, price manipulation, rate limits, provenance, and user privacy.

Produce security requirements before implementation.
```
