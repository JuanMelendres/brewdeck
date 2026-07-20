# Pull Request Review Checklist

## Intent and scope

- [ ] Requirement or issue understood
- [ ] Approved design identified
- [ ] Non-goals identified
- [ ] Diff matches intended scope
- [ ] Unrelated changes identified
- [ ] Generated files justified

## Correctness

- [ ] Main behavior
- [ ] Validation
- [ ] Null and boundary values
- [ ] Duplicates
- [ ] State transitions
- [ ] Transactions
- [ ] Concurrency
- [ ] Idempotency
- [ ] Error handling
- [ ] Time and numeric semantics

## API

- [ ] Paths and methods
- [ ] Request compatibility
- [ ] Response compatibility
- [ ] Status codes
- [ ] Error model
- [ ] Authentication
- [ ] Authorization
- [ ] Pagination
- [ ] Versioning
- [ ] Breaking changes documented

## Database

- [ ] ORM and schema aligned
- [ ] Existing data considered
- [ ] Released migrations unchanged
- [ ] Constraints and indexes
- [ ] Backfill
- [ ] Locking
- [ ] Deployment overlap
- [ ] Recovery

## Security

- [ ] Object-level authorization
- [ ] Input validation
- [ ] Secret safety
- [ ] Error leakage
- [ ] External input
- [ ] CORS and CSRF
- [ ] Dependency risk
- [ ] Container and CI safety

## Tests

- [ ] Happy path
- [ ] Validation
- [ ] Errors
- [ ] Permissions
- [ ] Not found
- [ ] Duplicates
- [ ] Transactions
- [ ] Concurrency
- [ ] External failures
- [ ] Migration path
- [ ] Frontend states
- [ ] Accessibility

## Performance and reliability

- [ ] N+1
- [ ] Pagination
- [ ] Bounded retries
- [ ] Timeouts
- [ ] Cache bounds
- [ ] Queue bounds
- [ ] Connection pools
- [ ] Graceful degradation
- [ ] Resource cleanup
- [ ] Observability

## Platform and documentation

- [ ] CI quality gates
- [ ] Docker and configuration
- [ ] Migration ownership
- [ ] Health verification
- [ ] Rollback and roll-forward
- [ ] FDD and TDD
- [ ] API docs
- [ ] Runbooks
- [ ] Release notes
