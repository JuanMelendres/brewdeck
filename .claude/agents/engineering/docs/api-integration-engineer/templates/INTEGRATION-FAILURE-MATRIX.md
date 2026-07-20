# Integration Failure Matrix

| Failure | Retry? | User/API result | Internal status | Metric | Recovery |
|---|---:|---|---|---|---|
| Connection timeout | Conditional | Temporary unavailable | UPSTREAM_TIMEOUT | integration_timeout_total | Retry with bounded backoff |
| 401 Unauthorized | No | Configuration or upstream auth error | UPSTREAM_UNAUTHORIZED | integration_auth_failure_total | Rotate or correct credentials |
| 429 Too Many Requests | Yes, respecting Retry-After | Temporarily rate limited | UPSTREAM_RATE_LIMITED | integration_rate_limited_total | Backoff, queue, or retry later |
| Malformed JSON | No | Upstream invalid response | UPSTREAM_INVALID_RESPONSE | integration_invalid_response_total | Preserve evidence and investigate |
| Partial page | Conditional | Partial import state | UPSTREAM_PARTIAL_DATA | integration_partial_data_total | Resume from checkpoint |
