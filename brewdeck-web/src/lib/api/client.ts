import { API_BASE_URL } from '@/config/env';
import { EMAIL_NOT_VERIFIED, notifyEmailNotVerified } from '@/lib/auth/emailVerificationSignal';
import { clearTokens, getToken, setToken } from '@/lib/auth/tokenStore';
import type { AuthResponse, ErrorResponse } from './types';

export class ApiError extends Error {
  status: number;
  path?: string;
  validationErrors?: Record<string, string>;
  code?: string;

  constructor(
    status: number,
    message: string,
    path?: string,
    validationErrors?: Record<string, string>,
    code?: string,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.path = path;
    this.validationErrors = validationErrors;
    this.code = code;
  }
}

/**
 * Sent on every request. The refresh cookie endpoints require it (ADR-013): a cross-site page
 * cannot add a custom header without a CORS preflight, so its presence proves a same-origin call.
 */
export const CSRF_HEADER: Record<string, string> = { 'X-Requested-With': 'fetch' };

/** Web Locks name shared by every tab of this origin. */
const REFRESH_LOCK = 'brewdeck-refresh';

let refreshInFlight: Promise<string> | null = null;

function isOnPublicPath(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const p = window.location.pathname;
  return (
    p === '/login' ||
    p === '/register' ||
    p === '/forgot-password' ||
    p === '/reset-password' ||
    p === '/verify-email' ||
    p.startsWith('/share')
  );
}

async function runRefresh(): Promise<string> {
  // The httpOnly refresh cookie authenticates this call; the body is empty.
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: CSRF_HEADER,
    credentials: 'same-origin',
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Refresh failed');
  }
  const auth = (await response.json()) as AuthResponse;
  setToken(auth.token);
  return auth.token;
}

/**
 * Gets a fresh access token from the refresh cookie. Rotation must never run twice at once:
 * a second call would present the already-rotated cookie and trip reuse detection, which revokes
 * every session. Within a tab, concurrent callers share one in-flight promise. Across tabs (which
 * share the cookie), a Web Lock queues them, so each tab rotates the latest cookie in turn.
 */
export function refreshSession(): Promise<string> {
  if (!refreshInFlight) {
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
    const run = locks
      ? async (): Promise<string> => await locks.request(REFRESH_LOCK, runRefresh)
      : runRefresh;
    refreshInFlight = run().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
  allowRefresh = true,
): Promise<T> {
  const token = getToken();
  const authHeader: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...CSRF_HEADER,
      ...authHeader,
      ...init?.headers,
    },
  });

  if (response.status === 401) {
    // Only an existing session can be renewed; a 401 from login etc. is just a failed attempt.
    const canRefresh = allowRefresh && path !== '/api/auth/refresh' && token !== null;

    if (canRefresh) {
      try {
        await refreshSession();
      } catch {
        clearTokens();
        if (typeof window !== 'undefined' && !isOnPublicPath()) {
          window.location.assign('/login');
        }
        throw new ApiError(401, 'Session expired');
      }
      // Retry OUTSIDE the try so the retried request's real errors propagate to the caller.
      return await apiFetch<T>(path, init, false);
    }

    clearTokens();
    if (typeof window !== 'undefined' && !isOnPublicPath()) {
      window.location.assign('/login');
    }
  }

  if (!response.ok) {
    let body: Partial<ErrorResponse> = {};
    try {
      body = (await response.json()) as ErrorResponse;
    } catch {
      // non-JSON error body; fall back to status text
    }
    if (response.status === 403 && body.code === EMAIL_NOT_VERIFIED) {
      notifyEmailNotVerified();
    }
    throw new ApiError(
      response.status,
      body.message ?? response.statusText,
      body.path,
      body.validationErrors,
      body.code ?? undefined,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
