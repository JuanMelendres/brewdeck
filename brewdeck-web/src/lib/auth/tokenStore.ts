/**
 * The access token lives only in memory (ADR-013): it is short-lived (15 min) and a page reload
 * gets a new one from the httpOnly refresh cookie, which scripts can never read. Nothing
 * token-related is persisted in the browser anymore.
 */
let accessToken: string | null = null;

/** Keys the pre-ADR-013 client stored in localStorage. */
const LEGACY_STORAGE_KEYS = ['brewdeck.token', 'brewdeck.refreshToken'];

export function getToken(): string | null {
  return accessToken;
}

export function setToken(token: string): void {
  accessToken = token;
}

export function clearTokens(): void {
  accessToken = null;
}

/**
 * Removes tokens an older version of the app left in localStorage, so a refresh token never
 * lingers where scripts can read it.
 */
export function purgeLegacyTokenStorage(): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    LEGACY_STORAGE_KEYS.forEach((key) => window.localStorage.removeItem(key));
  } catch {
    // Storage can be unavailable (privacy mode); there is nothing to purge then.
  }
}
