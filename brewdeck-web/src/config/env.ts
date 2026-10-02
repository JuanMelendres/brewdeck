/**
 * Base URL the browser uses for API calls. Empty = same origin: Next proxies /api/* to the
 * backend (see next.config.ts, ADR-013). Only set it to call a different origin on purpose; that
 * breaks the SameSite=Strict refresh cookie.
 */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? '';
