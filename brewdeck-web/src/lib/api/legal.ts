import { apiFetch } from './client';

/** Whether the privacy notice and terms are published, and the version a sign-up accepts. */
export type LegalStatus = { termsVersion: string; published: boolean };

/** Public: readable before login, so the sign-up page can ask for consent (LEGAL-01). */
export function getLegalStatus(): Promise<LegalStatus> {
  return apiFetch<LegalStatus>('/api/public/legal');
}
