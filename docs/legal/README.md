# Legal

Source texts and internal procedures for privacy and terms (backlog LEGAL-01). The decisions and
the law summary are in the [LEGAL-01 spike](../product/spikes/legal-01-privacy-terms-spike.md).

**Not legal advice.** A Mexican data-protection lawyer reviews the texts before they are published.

| File | What | Status |
|---|---|---|
| [`privacy-notice.md`](privacy-notice.md) | Full privacy notice (*aviso de privacidad integral*), Spanish | Draft for legal review |
| [`terms.md`](terms.md) | Terms and conditions, Spanish | Draft for legal review |

**Open points before publishing:**
- The post office (administración postal) and código postal of Apartado Postal 20456.
- Lawyer: whether a blocking period (Art. 24) is needed before deleting an account.
- Lawyer: whether the forum clause holds for consumers.
- English versions for `/privacy` and `/terms`. Spanish prevails.

## Versioning

- Each text carries a version date at the top. Publishing a new version means:
  1. update the date;
  2. bump `TERMS_VERSION` in the app (LEGAL-01 PR 2);
  3. if the change is material, email users and ask them to accept again at their next login.
- Never edit a published version in place. The git history is the record of what each user
  accepted (`users.terms_version`).

## Update the notice before...

- turning on a hosted AI provider in staging or prod. Recipe text would go to a new processor
  (Anthropic or another provider; ADR-006, ADR-016);
- adding analytics, error tracking or any third-party script or cookie;
- adding a secondary purpose (newsletter, marketing). It needs its own opt-out;
- adding a processor or moving data to a new country or provider;
- collecting a new kind of personal data.

## Processors

| Processor | Service | Data | Region |
|---|---|---|---|
| Google Cloud | Cloud Run (API), Cloud Logging, Secret Manager | All API traffic; request logs (IP, user agent) | us-central1 |
| Neon (on AWS) | PostgreSQL | All stored data | AWS us-east-2 |
| Vercel | Web hosting, Next.js proxy | Web traffic, cookies in transit | US |
| Google (Gmail SMTP) | Account emails (staging) | Recipient email, message content | US |

Production may change these (PROD-02 hosting, PROD-03 email provider). Update this table and the
notice in the same PR as the change.

## ARCO request procedure

Requests arrive at **jmelendresdev@gmail.com**. "Days" means business days (LFPDPPP Art. 2 VIII).

1. **Log it** the day it arrives, in a private log kept outside the repo, never in git. Record:
   - date received;
   - requester and account email;
   - the right asked (access, rectification, cancellation, opposition, revocation);
   - the due date: received + 20 business days.
2. **Check identity:**
   - The request must come from the account's email, or include an ID that matches.
   - A representative needs proof of representation.
   - If something is missing, ask once, within 5 business days. The clock restarts when it arrives.
3. **Decide and answer within 20 business days** (Art. 31). One extension of 20 more is allowed if
   justified; tell the requester before the first deadline.
4. **Carry it out within 15 business days** of the answer:
   - **Access:** export the user's rows (`users`, coffees, brew methods they own, recipes, brew
     sessions) as JSON. Exclude password hashes and token hashes. The self-service export comes
     with LEGAL-01 PR 3.
   - **Rectification:** the user can do most of it in the account page. Fix the rest by hand and
     record what changed.
   - **Cancellation or revocation:** delete the account and everything it owns. Until "Delete my
     account" ships (LEGAL-01 PR 3), do it by hand in Neon inside one transaction, starting from
     the child tables. Run the same steps as the future endpoint, and confirm by email.
   - **Opposition:** BrewDeck has no secondary purposes today, so explain that, or stop the use
     the request names.
5. **Close** the log entry with the date and what was done. Keep it for 2 years. *(Assumption;
   confirm with the lawyer.)*

The service is free. Charge nothing, except copies or shipping the user asks for (Art. 34).

## Security breach runbook

A breach means personal data was accessed, copied, changed, lost or destroyed without
authorization. Examples:
- a leaked database credential;
- a public bucket or log;
- a bug that showed one user's data to another.

1. **Contain** (hour 0):
   - rotate the exposed secret (DB password, `BREWDECK_JWT_SECRET`, SMTP password);
   - block the path, or roll forward a fix.

   Rotating `BREWDECK_JWT_SECRET` signs everyone out. That is expected.
2. **Assess** (same day):
   - which data;
   - which users;
   - since when;
   - whether it was actually accessed (Cloud Run logs, Neon logs).
3. **Notify the affected users at once** when the breach significantly affects their rights
   (Art. 19). Send an email with:
   - what happened;
   - which of their data;
   - what we did;
   - what they should do (for example, reset the password);
   - the contact address.
4. **Fix the cause** and add a regression test.
5. **Record it** in a postmortem (`incident-response-engineer` format): timeline, cause, data,
   users notified, actions. Keep it private if it has personal data.

## Retention (decided 2026-10-20)

| Data | Kept | Mechanism |
|---|---|---|
| Account and its content | Until the user deletes it | Self-service delete (LEGAL-01 PR 3); manual until then |
| Unverified accounts | 7 days | AUTH-06 purge job (pending) |
| Request logs (IP, user agent) | 30 days | Cloud Logging default retention |
| Expired or used tokens (reset, verification, refresh, email change) | 30 days | AUTH-06 purge job (pending) |
