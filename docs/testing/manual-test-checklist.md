# Manual Test Checklist

Checks that automated tests cannot cover: they need a real browser, a real inbox, staging data,
or the owner's judgement. Automated suites are described in [test-plan.md](test-plan.md).

**How to use it:**
- Run the checks on staging: web `https://brewdeck-staging.vercel.app`, API on Cloud Run
  (runbook: [staging-deployment.md](../development/staging-deployment.md)).
- Tick a check (`[x]`) and add the date. If it fails, leave it unticked, add a note, and open a
  backlog item.
- When a release adds manual checks, append a section here in the same PR. Delete a section once
  every check in it passes.

## Release #290: passwords (AUTH-03, ADR-018)

- [x] **Old account is upgraded to Argon2id.** Verified 2026-10-20 in the Cloud Run logs:
  `Upgraded password hash to Argon2id for user id=1`. To check another account on Neon:
  ```sql
  SELECT id, left(password_hash, 9) AS prefix FROM users WHERE id = <id>;  -- {argon2}$
  ```
- [ ] **Minimum length.** Sign up with a 14-character password.
  Expected: "La contraseña debe tener al menos 15 caracteres" under the field; no account created.
- [ ] **Common password.** Sign up with `PasswordPassword`.
  Expected: "Esta contraseña es demasiado común. Elige otra" under the field.
- [ ] **Valid passphrase.** Sign up with a passphrase of 15+ characters, including accents or an
  emoji. Expected: the "check your email" screen.
- [ ] **Change password** (Account page) and **reset password** (forgot-password link) apply the
  same three rules.
- [ ] **Long password.** A 128-character password is accepted; 129 characters gives a field error.
- [ ] **Show/hide toggle** works on login, sign up, change password and reset password. The eye
  button switches the field between hidden and visible, and the screen reader announces its
  pressed state.
- [ ] **Old short password still logs in.** An account created before the release with an
  8-character password can still log in (the minimum only applies when a password is set).

## Release #272: auth hardening, brew sessions, numeric precision

### Email verification (AUTH-02, ADR-012)
- [ ] **Blocked until verified.** Sign up with a new email.
  Expected: the "Verifica tu correo para continuar" screen; coffees, recipes and sessions stay
  blocked.
- [ ] **The verification email arrives** (check spam). Its link verifies the account and unblocks it.
- [ ] **Resend** works, and a fourth resend within an hour is refused (limit 3 per hour).

### No account enumeration (AUTH-05.7)
- [ ] Sign up again with an already registered email.
  Expected: the same "check your email" screen as a new signup. The existing owner gets an
  "account exists" email instead of a verification link.

### Sessions (AUTH-05.6)
- [ ] **Logout revokes the access token.**
  1. In DevTools → Network, copy the `Authorization: Bearer …` header from an `/api/` request.
  2. Log out.
  3. Replay the request:
     ```bash
     curl -s -o /dev/null -w '%{http_code}\n' -H 'Authorization: Bearer <token>' <API_URL>/api/coffees
     ```
  Expected: `401`.
- [ ] **Password change logs out other devices.** Log in on two browsers, change the password in
  one. Expected: the other one is sent to login on its next action.

### Security headers (AUTH-05.1)
- [ ] In DevTools → Network, open the main document of any page. Expected:
  - `X-Frame-Options: DENY`
  - `X-Content-Type-Options: nosniff`
  - `Content-Security-Policy-Report-Only`
  - `Referrer-Policy: no-referrer` on `/reset-password` and `/verify-email`

### Numbers (DATA-01)
- [ ] **Derived ratio.** Create a recipe with 15 g of coffee and 250 g of water.
  Expected: ratio `1:16.7` (`1:16,7` in Spanish).
- [ ] **Decimal temperature.** Recipe water temperature `92.5`.
  Expected: saved as is and shown as `92,5 °C` in Spanish and `92.5 °C` in English.
- [ ] **Decimal comma input (fix 4).** Type `15,5` in the grams and temperature fields:

  | Browser | Desktop | Phone |
  |---|---|---|
  | Chrome | [ ] | [ ] |
  | Safari | [ ] | [ ] |
  | Firefox | [ ] | — |

  Expected: the value is kept as 15.5. If a browser drops it or saves 155, report it; the fix is a
  text input with `inputMode="decimal"` that accepts a comma or a dot.
- [ ] **Recipe PDF** in Spanish shows `15,5 g` and `92,5 °C`.

### Brew sessions and guidance (BREW-01, UX-02)
- [ ] **Session detail.** Open a brew session: every field shows, and the recipe link works.
- [ ] **Edit and delete** work. Delete asks for confirmation.
- [ ] **Phone.** Brew sessions show as cards, not a table.
- [ ] **New account guidance.** With no coffees, "Add recipe" is disabled. The empty state opens the
  coffee form, and saving it opens the recipe form with that coffee selected.

## Local only

- [ ] **Ollama after the Jackson 3 port (OPS-01).** With `ollama serve` running locally and
  `AI_PROVIDER=ollama`, run one "Suggest with AI" and one "Improve with AI".
  Expected: both return a recipe; no JSON errors in the API log.

## Operations watch

- [ ] **Memory after the 1 GiB change (2026-10-20).** For a week, search the Cloud Run logs for
  `Memory limit` on `brewdeck-api-staging`. Expected: no matches. If it shows up, try
  `--memory=1536Mi` (runbook §5.4).
