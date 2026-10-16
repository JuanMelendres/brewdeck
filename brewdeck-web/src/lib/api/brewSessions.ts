import { apiFetch } from './client';
import type { BrewSession, BrewSessionFilters, PageResponse } from './types';
import type { BrewSessionFormValues } from '@/lib/validation/brewSessionSchema';

export type ListBrewSessionsParams = {
  page: number;
  size: number;
  sort?: string;
  filters?: BrewSessionFilters;
};

export function listBrewSessions(
  params: ListBrewSessionsParams,
): Promise<PageResponse<BrewSession>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  const filters = params.filters ?? {};
  if (filters.rating !== undefined) {
    query.set('rating', String(filters.rating));
  }

  return apiFetch<PageResponse<BrewSession>>(`/api/brew-sessions?${query.toString()}`);
}

export function getBrewSession(id: number): Promise<BrewSession> {
  return apiFetch<BrewSession>(`/api/brew-sessions/${id}`);
}

export function createBrewSession(body: BrewSessionFormValues): Promise<BrewSession> {
  return apiFetch<BrewSession>('/api/brew-sessions', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateBrewSession(id: number, body: BrewSessionFormValues): Promise<BrewSession> {
  return apiFetch<BrewSession>(`/api/brew-sessions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function deleteBrewSession(id: number): Promise<void> {
  return apiFetch<void>(`/api/brew-sessions/${id}`, { method: 'DELETE' });
}

export type ListByRecipeParams = { page: number; size: number; sort?: string };

export function listBrewSessionsByRecipe(
  recipeId: number,
  params: ListByRecipeParams,
): Promise<PageResponse<BrewSession>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'brewedAt,desc');

  return apiFetch<PageResponse<BrewSession>>(
    `/api/brew-sessions/recipe/${recipeId}?${query.toString()}`,
  );
}
