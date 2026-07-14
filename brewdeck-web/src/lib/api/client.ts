import { API_BASE_URL } from '@/config/env';
import {
  clearTokens,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from '@/lib/auth/tokenStore';
import type { AuthResponse, ErrorResponse } from './types';

export class ApiError extends Error {
  status: number;
  path?: string;
  validationErrors?: Record<string, string>;

  constructor(
    status: number,
    message: string,
    path?: string,
    validationErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.path = path;
    this.validationErrors = validationErrors;
  }
}

let refreshInFlight: Promise<AuthResponse> | null = null;

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

async function runRefresh(refreshToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Refresh failed');
  }
  return (await response.json()) as AuthResponse;
}

// Collapses concurrent 401s into a single rotation so we never double-fire /refresh
// (a second rotation would present an already-used token and trip reuse detection).
function attemptRefresh(refreshToken: string): Promise<AuthResponse> {
  if (!refreshInFlight) {
    refreshInFlight = runRefresh(refreshToken).finally(() => {
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
    headers: { 'Content-Type': 'application/json', ...authHeader, ...init?.headers },
  });

  if (response.status === 401) {
    const refreshToken = getRefreshToken();
    const canRefresh = allowRefresh && path !== '/api/auth/refresh' && refreshToken !== null;

    if (canRefresh) {
      try {
        const auth = await attemptRefresh(refreshToken as string);
        setToken(auth.token);
        setRefreshToken(auth.refreshToken);
        // Retry the original request once; disallow a further refresh to avoid loops.
        return await apiFetch<T>(path, init, false);
      } catch {
        // Refresh failed — fall through to clear + redirect below.
      }
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
    throw new ApiError(
      response.status,
      body.message ?? response.statusText,
      body.path,
      body.validationErrors,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
