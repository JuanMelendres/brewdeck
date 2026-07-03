import { API_BASE_URL } from '@/config/env';
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
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });

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
