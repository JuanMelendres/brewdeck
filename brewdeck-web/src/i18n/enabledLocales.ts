import { DEFAULT_LOCALE, isLocale, type AppLocale } from './config';

// Read on the Next server only: the browser never needs it, and API_PROXY_TARGET is server-side.
const API_TARGET = process.env.API_PROXY_TARGET ?? 'http://localhost:8080';

/**
 * Languages the server may render, from the backend's `web-i18n-spanish` flag
 * (GET /api/public/ui-config, docs/architecture/i18n-spanish-rollout-tdd.md). Cached for 60 s.
 * Any failure means English only, so an API outage never breaks rendering.
 */
export async function enabledLocales(): Promise<AppLocale[]> {
  try {
    const response = await fetch(`${API_TARGET}/api/public/ui-config`, {
      next: { revalidate: 60 },
      signal: AbortSignal.timeout(2000),
    });
    if (!response.ok) {
      return [DEFAULT_LOCALE];
    }
    const body: unknown = await response.json();
    const languages =
      typeof body === 'object' && body !== null && 'languages' in body && Array.isArray(body.languages)
        ? body.languages.filter(isLocale)
        : [];
    return languages.includes(DEFAULT_LOCALE) ? languages : [DEFAULT_LOCALE, ...languages];
  } catch {
    return [DEFAULT_LOCALE];
  }
}
