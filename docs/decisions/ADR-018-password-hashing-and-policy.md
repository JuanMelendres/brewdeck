# ADR-018: Argon2id password hashing and a NIST-style password policy

## Status
Accepted (2026-10-19). Backlog AUTH-03.

## Date
2026-10-19

## Context
- Passwords are hashed with BCrypt (Spring's default cost 10). BCrypt only reads the first 72
  bytes, so the API caps passwords at 72 UTF-8 bytes (`@MaxUtf8Bytes(72)`, audit item 4). With
  accented letters or emoji, a passphrase hits that cap well before 64 characters.
- The minimum length is 8. There are no composition rules and no forced rotation. Credentials
  are revoked on change and reset (ADR-013, AUTH-05.6).
- NIST SP 800-63B, for passwords used as the only authentication factor (BrewDeck has no MFA):
  - minimum 15 characters;
  - accept at least 64 characters;
  - check new passwords against a list of common or compromised values;
  - no composition rules and no periodic rotation;
  - allow showing the password while typing.
- The owner decided on 2026-10-19: Argon2id, minimum 15, and a local blocklist.

## Decision
**Hashing (PR 1).**
- New hashes use **Argon2id** with OWASP's minimum profile: m = 19 MiB, t = 2, p = 1, 16-byte
  salt, 32-byte hash. The implementation is Spring Security's `Argon2PasswordEncoder` on
  BouncyCastle.
- A `DelegatingPasswordEncoder` writes `{argon2}...`. Hashes without a prefix (every existing
  BCrypt hash) still match through BCrypt.
- **Upgrade on login:** after a successful login, a hash that `upgradeEncoding` reports as outdated
  is re-hashed with Argon2id and saved. Accounts that never log in again keep BCrypt, which is
  still a sound algorithm.
- **Memory bound:** each Argon2id hash allocates about 19 MiB on the heap. The staging container
  has 512 MiB, about 384 MiB of heap. Hashing goes through a fair semaphore of 4 permits, so a
  login burst queues instead of exhausting memory. Login rate limits (ADR-011) already cap
  attempts per IP and account.
- **Length cap:** passwords may be up to **128 characters**. That is above NIST's 64 and bounds
  the work per request. The 72-byte rule goes away.
  - A login over 72 bytes against an account that still has a BCrypt hash is a plain
    non-match, not an error.

**Policy (PR 2).**
- Minimum **15 characters** for new passwords: register, change and reset. Existing passwords
  keep working; the rule applies only when a password is set.
- A **local blocklist** of common passwords ships with the API and is compared case-insensitively.
  Nothing is sent to a third party. The same list is not shipped to the browser; the API answers
  with a field error.
- The web adds a show/hide toggle on password fields, and Zod mirrors the rules.

## Consequences
- **Positive:**
  - Memory-hard hashing.
  - Passphrases of any script up to 128 characters.
  - Existing users migrate without a reset.
  - The policy follows NIST.
- **Negative:**
  - A new dependency (BouncyCastle `bcprov`), scanned by OWASP Dependency-Check.
  - About 50 to 100 ms of CPU and 19 MiB per hash.
  - While some accounts still hold BCrypt hashes, a login for such an account costs a different
    amount of time than one for an unknown email, which is checked against an Argon2id dummy
    hash. This leaks "this account has not logged in since the upgrade". It fades as accounts
    migrate. **Assumption:** this is acceptable for staging.
- **Rollback:** Argon2id hashes need the Argon2 encoder to verify. Going back to plain BCrypt
  would lock out every user who logged in after this change. Keep the `DelegatingPasswordEncoder`
  even if the default changes.

## Alternatives considered
- **BCrypt with pre-hashing** (HMAC or SHA-256 before BCrypt) to lift the 72-byte limit. It is a
  home-made scheme, and the plain SHA-256 variant carries known "password shucking" risks.
  Rejected in favour of a standard memory-hard algorithm.
- **Keep BCrypt and the 72-byte cap.** It leaves multibyte passphrases short. Rejected.
- **Have I Been Pwned (k-anonymity) for the blocklist.** It has better coverage, but it puts an
  external call in every signup and sends hash prefixes to a third party. Rejected for now; it
  could be added later behind a flag.
