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

export function createBrewSession(body: BrewSessionFormValues): Promise<BrewSession> {
  return apiFetch<BrewSession>('/api/brew-sessions', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
