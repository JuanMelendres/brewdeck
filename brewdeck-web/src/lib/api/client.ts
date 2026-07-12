import { API_BASE_URL } from '@/config/env';
import { clearToken, getToken } from '@/lib/auth/tokenStore';
import type { ErrorResponse } from './types';

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

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const authHeader: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...authHeader, ...init?.headers },
  });

  if (response.status === 401) {
    clearToken();
    if (typeof window !== 'undefined') {
      const p = window.location.pathname;
      const onPublic =
        p === '/login' ||
        p === '/register' ||
        p === '/forgot-password' ||
        p === '/reset-password' ||
        p.startsWith('/share');
      if (!onPublic) {
        window.location.assign('/login');
      }
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
