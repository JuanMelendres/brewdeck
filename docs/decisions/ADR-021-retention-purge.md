# ADR-021: Retention purge of unverified accounts and old tokens

## Status
Accepted (2026-10-20). Backlog AUTH-06.

## Date
2026-10-20

## Context
- The privacy notice (LEGAL-01) promises:
  - accounts that never verified their email are deleted after 7 days;
  - expired or used single-use links and session tokens are deleted after 30 days.

  Nothing deletes them today (`docs/legal/README.md`, "Retention").
- Email verification (ADR-012) blocks unverified users only where the
  `auth-require-email-verification` flag is on: staging today; prod is still off.
- `users` has `email_verified` (boolean) and `created_at`. Accounts that existed before
  verification shipped were marked verified (V10).
- Token tables all have `expires_at`:
  - `refresh_tokens` (7 days);
  - `password_reset_tokens`;
  - `email_verification_tokens` (24 hours);
  - `email_change_requests` (1 hour).
- Staging runs on Cloud Run with `--min-instances=0`, so an in-process `@Scheduled` task does not
  run while nobody uses the app.

## Decision

**Trigger: a Cloud Run Job.**
- The job runs the same API image in a one-shot mode (`brewdeck.retention.run-once=true`). It runs
  the purge, logs the counts, and exits 0, or non-zero on failure.
  - The web server still starts, because the security configuration needs a web context. A Cloud
    Run Job has no ingress, so nothing can reach it.
  - Queued emails finish before exit (`spring.task.execution.shutdown.await-termination`).
- Cloud Scheduler starts the job once a day. No HTTP endpoint is exposed.
- The deploy pipeline points the job at the new image after the service is promoted, so the job
  always runs the schema the service migrated.
- Locally: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--brewdeck.retention.run-once=true`.
- One run at a time: the scheduler starts one execution a day, and every step is idempotent.

**Unverified accounts (owner, 2026-10-20).**
- Only in environments where `auth-require-email-verification` is on. Where it is off, unverified
  users may use the app, so their accounts are kept.
- Reminder on day 5:
  - An unverified account created more than 5 days ago, with no reminder yet, gets a reminder email
    in its language with a fresh verification link (24 h, ADR-012) and the deletion date.
  - The send is recorded in `users.retention_reminder_sent_at` (new column).
  - The reminder is best-effort. A mail failure is logged, and the send still counts.
- Deletion: an unverified account is deleted when it was created more than 7 days ago **and** its
  reminder went out at least 2 days ago.
  - Every account gets the reminder before deletion, including old accounts found the first time
    the flag is turned on.
  - The deletion uses the same order and transaction as "Delete my account" (ADR-020, shared code),
    one account per transaction.
  - No email after deletion: the address was never proven to belong to the person.
- Verified accounts are never touched. Verifying at any time, including after the reminder, stops
  the deletion.

**Old tokens (always on, every environment).**
- Delete rows whose `expires_at` is more than 30 days ago from the four token tables.
- Every token is used or revoked before it expires, so one rule covers "expired or used". A used
  refresh token outlives its expiry by 30 days, so reuse detection (ADR-013) keeps working while
  the token could still be valid.

**Limits and clock.**
- Each run handles at most 500 accounts and deletes tokens in batches of 1000, so a backlog drains
  over several days without long locks.
- Time comes from an injected `Clock`. Tests use a fixed clock.

**No feature flag for the job.** The account rule already follows
`auth-require-email-verification`, and the token rule is plain retention.

## Consequences
- **Positive:**
  - The retention promises in the privacy notice become true.
  - No public endpoint is added, and no instance has to stay warm.
- **Negative:**
  - New infrastructure: a Cloud Run Job and a Cloud Scheduler trigger.
    - One-time setup in `docs/development/staging-deployment.md`.
    - The job needs the same secrets as the service.
  - A new email template (en/es) and a new column (V37).
  - Deleted accounts cannot be restored, except through Neon point-in-time restore of the whole
    database.
- **Prod:** turning on `auth-require-email-verification` in prod also starts the reminders, and
  deletions 2 days later. The go-live checklist must say so.

## Alternatives considered
- **`@Scheduled` inside the API.** No new infrastructure, but it only runs while an instance is up,
  so the 7-day promise would not hold. Rejected.
- **A protected endpoint called by Cloud Scheduler.** Simpler to deploy, but it adds an endpoint
  and a shared secret to a public service (PROD-01 is still open). Rejected.
- **Purge unverified accounts everywhere.** It would delete real users in prod while verification
  is optional there. Rejected by the owner.
- **Delete without a reminder.** Less code, but a user who missed the first email loses the
  account silently. Rejected by the owner.
- **A `status` column (`PENDING_VERIFICATION`, AUTH-02).** `email_verified` plus
  `retention_reminder_sent_at` is enough for this rule. Deferred.
