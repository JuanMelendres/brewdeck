# External API Contract Review Checklist

## Contract

- [ ] Upstream documentation identified
- [ ] API version identified
- [ ] Authentication documented
- [ ] Base URL configurable
- [ ] Methods and paths verified
- [ ] Headers verified
- [ ] Request schema verified
- [ ] Response schema verified
- [ ] Required and optional fields verified
- [ ] Nullability verified
- [ ] Date and identifier formats verified
- [ ] Error statuses verified

## Resilience

- [ ] Connect timeout
- [ ] Response timeout
- [ ] Overall deadline
- [ ] Retry classification
- [ ] Bounded attempts
- [ ] Exponential backoff
- [ ] Jitter
- [ ] Retry-After support
- [ ] Idempotency
- [ ] Circuit breaker only when justified
- [ ] Graceful fallback

## Rate limits and pagination

- [ ] Quotas documented
- [ ] Burst limits documented
- [ ] Remaining/reset headers handled
- [ ] Backpressure
- [ ] Maximum page safety limit
- [ ] Empty page
- [ ] Duplicate records across pages
- [ ] Resume or checkpoint strategy

## Data safety

- [ ] Transport DTO separated from domain
- [ ] Required fields validated
- [ ] Unknown fields policy
- [ ] Partial data policy
- [ ] Duplicate handling
- [ ] Provenance
- [ ] Freshness
- [ ] User-owned data protected

## Security

- [ ] Credential remains server-side
- [ ] No secrets logged
- [ ] TLS verification enabled
- [ ] Redirect policy reviewed
- [ ] SSRF reviewed
- [ ] URL validation
- [ ] Sensitive payload minimization
- [ ] Error body sanitization

## Testing

- [ ] Success
- [ ] 401
- [ ] 403
- [ ] 404
- [ ] 429
- [ ] Retry-After
- [ ] Timeout
- [ ] 5xx
- [ ] Malformed response
- [ ] Missing fields
- [ ] Wrong content type
- [ ] Pagination
- [ ] Duplicate data
- [ ] Partial data
