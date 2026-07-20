# BrewDeck security prompts

## BrewDeck authorization review

```text
@security-auditor Review BrewDeck's authorization model.

Trace access to coffees, grinders, brew methods, recipes, and brew sessions.
Verify that one authenticated user cannot read, update, delete, or reference
another user's resources by changing UUIDs.

Inspect controllers, services, repositories, queries, and tests.
Do not modify files.
```

## BrewDeck API security

```text
@security-auditor Review the BrewDeck API security configuration.

Evaluate:
- Spring Security endpoint matchers
- authentication requirements
- object ownership
- CORS for the Next.js frontend
- CSRF decisions
- validation and request limits
- error responses
- Actuator exposure
- sensitive logging
- feature-flagged incomplete endpoints

Do not modify files.
```

## Future AI Barista threat review

```text
@security-auditor Threat-model the future BrewDeck AI Barista feature.

Cover:
- prompt injection
- tool authorization
- access to user recipes and sessions
- cross-user data leakage
- untrusted tasting notes
- external coffee data
- logging and retention
- rate limits
- human confirmation before write actions

Produce architecture and test handoffs.
Do not modify files.
```
