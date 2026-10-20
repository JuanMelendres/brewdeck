-- RELEASE flag for the privacy notice and terms (backlog LEGAL-01). When on, the web links
-- /privacy and /terms, sign-up shows the simplified notice and requires accepting the current
-- version, and account emails link the privacy notice.
--
-- Disabled EVERYWHERE until a lawyer approves the texts in brewdeck-web/content/legal/.
--
-- Owner: Juan (product owner). Expires: 2027-03-31. Removal condition: enabled in staging and
-- prod after the legal review, stable for 2 weeks; then make the consent unconditional.
INSERT INTO feature_flags
    (feature_key, display_name, description, environment, enabled, flag_type, owner, expires_at, removal_condition)
SELECT 'legal-terms',
       'Privacy notice and terms',
       'Publishes the privacy notice and terms and requires accepting them at sign-up.',
       env, FALSE, 'RELEASE', 'Juan (product owner)', TIMESTAMP '2027-03-31 00:00:00',
       'Enabled in staging and prod after legal review, stable for 2 weeks; then remove the flag.'
  FROM (VALUES ('local'), ('dev'), ('test'), ('staging'), ('prod')) AS environments(env);
