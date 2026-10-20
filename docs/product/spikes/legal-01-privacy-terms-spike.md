# LEGAL-01 spike: privacy notice, terms, and data rights

**Status:** decisions taken (owner, 2026-10-20); texts pending. Backlog LEGAL-01. Not legal advice: a Mexican data-protection
lawyer reviews the final texts before production (PROD go-live).

## 1. Which law applies

- BrewDeck is run from Mexico by a private party, so the **Ley Federal de Protección de Datos
  Personales en Posesión de los Particulares (LFPDPPP)** applies.
  - The current law was published in the DOF on 2025-03-20 and took effect on 2025-03-21. It
    replaced the 2010 law. Later reform: DOF 2025-11-14.
  - The authority is the **Secretaría Anticorrupción y Buen Gobierno** (Art. 2 XV), which replaced
    the INAI.
- Points from the 2025 text that matter here:
  - "Días" means business days (Art. 2 VIII).
  - **Tacit consent is valid** once the privacy notice is available (Art. 7). Express consent is
    only needed for financial data (Art. 7) and express written consent for sensitive data
    (Art. 8). BrewDeck collects neither.
  - **The notice must say** (Art. 15):
    - who is responsible, with an address;
    - which data is processed;
    - the purposes, separating those that need consent;
    - how to limit use;
    - how to exercise ARCO rights;
    - how changes are announced.
  - Data collected online may use a **simplified notice** (Art. 15 I–IV) that points to the full
    one (Art. 16).
  - **ARCO requests:**
    - The request carries name, contact, ID, the data concerned and the right (Art. 28).
    - The answer is due in **20 business days**, extendable once by the same period. The decision
      takes effect within **15 business days** (Art. 31).
    - Requests are free, except copies and shipping (Art. 34).
  - **Retention:** delete data once its purpose is done, after a **blocking** period (Arts. 10, 24).
  - **Security:** administrative, technical and physical measures (Art. 18). A breach that
    significantly affects people must be **told to them immediately** (Art. 19).
  - **Processors:** hosting and email providers process data on BrewDeck's behalf (Art. 2 XII).
    The transfer rules do not apply to them (Art. 35).
  - **Fines:** from 100 to 320,000 UMA, doubled for sensitive data (Art. 59).
- **Terms and conditions** are not required by the LFPDPPP. They set the rules of use, the limits
  of liability, and the right to close accounts.

## 2. What BrewDeck processes today

| Data | Where | Purpose |
|---|---|---|
| Email, password hash (Argon2id), display name, language, theme, email-verified flag | `users` (Neon, AWS us-east-2) | Account and login |
| Coffees, recipes, brew sessions, ratings, notes | Neon | The service itself |
| Refresh, reset, verification and email-change tokens (hashes) | Neon | Sessions and account security |
| IP address, user agent, request path | Cloud Run request logs (Google, us-central1); in-memory rate limits | Security, abuse prevention, debugging |
| Cookies: `brewdeck_refresh` (httpOnly session), `brewdeck-locale` (language); local storage for theme | Browser | Strictly necessary or preference. No analytics, no ads |
| Emails sent (verification, reset, account notices) | Gmail SMTP (staging) | Account messages |
| Recipe text sent to an AI model | Local Ollama only (ADR-016); off in staging | Optional suggestions |

**Processors (Art. 2 XII):**
- Google Cloud (Cloud Run, logs; US).
- Neon (database; AWS US).
- Vercel (web hosting; US).
- Google (Gmail SMTP).

No sensitive or financial data. Data is stored outside Mexico. The full notice should say so, even
though the 2025 law no longer requires listing transfers.

## 3. Owner decisions (2026-10-20)

| # | Decision | Value |
|---|---|---|
| 1 | Responsable | **Juan Melendres, as a natural person (persona física).** Move to a company (for example an S.A.S.) if BrewDeck charges or grows. |
| 1a | Address for notices (Art. 15 I) | **Apartado Postal 20456, Aguascalientes, Ags., México.** **TODO (owner):** confirm the post office (administración postal) and its código postal, so the address is complete in the notice. |
| 2 | Privacy, ARCO and account-recovery contact | **jmelendresdev@gmail.com.** It is also the recovery contact of ADR-019 ("Known limits"). Move to `privacidad@<domain>` once the custom domain exists (PROD-02/PROD-03). |
| 3 | Minimum age | **18+.** The terms say so, and sign-up states it. No minors' data, so no parental consent flow. |
| 4 | Retention | Account and all its data **deleted at once** on request. Unverified accounts **purged after 7 days** (AUTH-06). Cloud Run logs kept the default **30 days**. Expired or used tokens deleted after **30 days**. **Open for the lawyer:** whether a blocking period (Art. 24) is needed before deletion. |
| 5 | Governing law and courts (terms) | **Mexican federal law; the competent courts of the city of Aguascalientes, Aguascalientes.** |

Rationale for each option is in the PR #299 discussion: a natural person is free and fast; a
company shields personal assets but costs money and paperwork.

## 4. Where it goes

| Item | Place | Notes |
|---|---|---|
| Full privacy notice (*aviso integral*) | Public page `/privacy` (es + en) | Versioned with a date |
| Terms and conditions | Public page `/terms` (es + en) | Versioned with a date |
| Simplified notice | Under the sign-up button | One paragraph (Art. 15 I–IV) with links to `/privacy` and `/terms` |
| Acceptance record | `users.terms_version`, `users.terms_accepted_at` (new migration) | Ask again when the version changes |
| Footer links | `AuthLayout` and the app layout | "Aviso de privacidad · Términos" |
| Emails | Footer of every email | Link to `/privacy` |
| ARCO self-service | Account page | **Access:** "Download my data" (JSON). **Rectification:** profile and email change already exist. **Cancellation:** "Delete my account" (re-authenticate, confirm, cascade delete). **Opposition:** the contact email |
| Internal process | `docs/legal/` | ARCO request log and steps (20 business days); breach runbook (Art. 19); processor list |
| Source texts | `brewdeck-web/content/legal/{privacy,terms}.{es,en}.md` (moved from `docs/legal/` in PR 2) | The web renders these files; i18n keys hold only short texts |

## 5. Draft: simplified notice (sign-up)

> **Aviso de privacidad simplificado.** Juan Melendres, con domicilio para oír y recibir notificaciones en el Apartado Postal 20456, Aguascalientes, Ags.,
> trata tu correo, nombre visible, preferencias y los datos de café que registras para darte una
> cuenta y operar BrewDeck. No usamos tus datos para publicidad. Consulta el aviso de privacidad
> integral, incluido cómo ejercer tus derechos ARCO, en [/privacy], o escribe a
> jmelendresdev@gmail.com. Debes tener 18 años o más. Al crear tu cuenta aceptas los [Términos].

## 6. Draft outline: full notice (`/privacy`)

1. Identity and address of the responsable; privacy contact.
2. Data processed (table in §2), with no sensitive or financial data.
3. **Primary purposes** (no consent needed beyond the notice): account, login, security, the
   service, account emails. **Secondary purposes:** none today. If added (for example a
   newsletter), they need a separate opt-out.
4. Processors and where they host (US), with security measures in summary.
5. Cookies and browser storage: necessary and preference only. No analytics or ads.
6. ARCO rights and consent revocation:
   - self-service in the account page;
   - otherwise email with the Art. 28 contents;
   - 20 + 15 business-day deadlines (Art. 31);
   - free.
7. How to limit use or disclosure (the account settings; the contact email).
8. Retention and deletion (decision 4).
9. Minimum age (decision 3).
10. Changes to the notice: announced on the site and by email, with the version date.
11. The authority: Secretaría Anticorrupción y Buen Gobierno.

**Terms (`/terms`) outline:**
1. The service, free and offered as is.
2. Account: one per person, 18+, keep the password safe.
3. Acceptable use: no abuse, scraping or attacks; shared recipes must be the user's own content.
4. User content: the user keeps ownership and grants a license to host and show what they share.
5. AI suggestions are informative, not guaranteed.
6. Account closure by the user or the operator.
7. Limitation of liability.
8. Changes to the terms.
9. Governing law.
10. Contact.

## 7. Delivery plan

1. **Owner:** decisions 1–5 taken on 2026-10-20. Still open: the post office and código postal of
   the P.O. box (1a).
2. **PR 1 (docs, done as drafts 2026-10-20, #300):** privacy notice and terms drafts (es; now in `brewdeck-web/content/legal/`) with the
   decisions filled in, plus the processor list and the ARCO and breach steps. **Lawyer review.**
3. **PR 2 (full-stack, built 2026-10-20 behind `legal-terms`):**
   - `/privacy` and `/terms` pages and footer links;
   - the simplified notice at sign-up;
   - a migration with `terms_version` / `terms_accepted_at`, recorded on register;
   - an email footer link.
4. **PR 3 (full-stack):** "Download my data" (`GET /api/auth/me/export`) and "Delete my account"
   (`DELETE /api/auth/me` with the password, cascade delete, a confirmation email). ADR first,
   because deletion touches every table.
5. Staging needs at least PR 2 if sign-up stays open to the public.

## Sources

- Cámara de Diputados, LFPDPPP consolidated text: https://www.diputados.gob.mx/LeyesBiblio/pdf/LFPDPPP.pdf
- Reform index: https://www.diputados.gob.mx/LeyesBiblio/ref/lfpdppp.htm
- Orden Jurídico Nacional, 2025 decree text: https://www.ordenjuridico.gob.mx/Documentos/Federal/html/wo125102.html
- EY, entry into force: https://www.ey.com/es_mx/technical/tax/boletines-fiscales/nueva-ley-federal-proteccion-datos-personal-posesion-particulares
- Garrigues, summary of changes: https://www.garrigues.com/es_ES/noticia/mexico-nueva-ley-federal-proteccion-datos-personales-posesion-particulares-introduce
