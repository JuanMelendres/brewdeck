import { ApiError } from './client';

/**
 * For a 429 from the auth rate limiter, the server's message (e.g. "Too many attempts. Try again in
 * 3 minutes."); otherwise null, so callers fall back to their own wording.
 */
export function rateLimitMessage(error: unknown): string | null {
  return error instanceof ApiError && error.status === 429 ? error.message : null;
}
