# Review Severity Rubric

## Blocker

Must not merge.

Typical impact:

- Data loss or corruption
- Security bypass
- Secret exposure
- Broken migration or deployment
- Core feature does not work
- Unplanned breaking contract
- Likely production outage

## Major

Fix before merge.

Typical impact:

- Important error path broken
- Missing authorization
- Unsafe concurrency or retry
- Existing-data failure
- Major regression lacks protection
- Unsafe operational behavior

## Moderate

Usually fix before merge or explicitly accept.

Typical impact:

- Real maintainability or reliability risk
- Missing meaningful edge case
- Incomplete observability
- Scale-dependent performance issue
- Documentation mismatch

## Minor

Non-blocking.

Typical impact:

- Small clarity issue
- Local duplication
- Test readability
- Documentation polish
- Low-risk hardening

## Suggestion

Optional idea without merge requirement.

## Rules

- Severity reflects impact and likelihood.
- Difficulty of remediation does not reduce severity.
- Personal style is not a finding.
- Every finding requires evidence and a realistic failure scenario.
