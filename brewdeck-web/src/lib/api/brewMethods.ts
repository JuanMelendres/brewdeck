import { apiFetch } from './client';
import type { PageResponse } from './types';
import type { BrewMethodFormValues } from '@/lib/validation/brewMethodSchema';

/**
 * A brew method as seen by the current user: `shared` is true for the admin-managed catalog
 * (read-only here) and false for the user's own private methods.
 */
export type BrewMethod = {
  id: number;
  name: string;
  description: string | null;
  shared: boolean;
  createdAt: string;
};

export type ListBrewMethodsParams = { page: number; size: number };

export function listBrewMethods(params: ListBrewMethodsParams): Promise<PageResponse<BrewMethod>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  return apiFetch<PageResponse<BrewMethod>>(`/api/brew-methods?${query.toString()}`);
}

/** Creates a private method owned by the current user. */
export function createBrewMethod(body: BrewMethodFormValues): Promise<BrewMethod> {
  return apiFetch<BrewMethod>('/api/brew-methods', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

/** Updates one of the current user's private methods (403 for shared methods). */
export function updateBrewMethod(id: number, body: BrewMethodFormValues): Promise<BrewMethod> {
  return apiFetch<BrewMethod>(`/api/brew-methods/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

/** Deletes one of the current user's private methods (409 while recipes still use it). */
export function deleteBrewMethod(id: number): Promise<void> {
  return apiFetch<void>(`/api/brew-methods/${id}`, { method: 'DELETE' });
}

export type MethodUsage = {
  methodId: number;
  methodName: string;
  recipeCount: number;
};

export function listMethodUsage(): Promise<MethodUsage[]> {
  return apiFetch<MethodUsage[]>('/api/brew-methods/usage');
}
