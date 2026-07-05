import { apiFetch } from './client';
import type { PageResponse } from './types';

export type BrewMethod = {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string | null;
};

export function listBrewMethods(params: {
  page: number;
  size: number;
}): Promise<PageResponse<BrewMethod>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  return apiFetch<PageResponse<BrewMethod>>(`/api/brew-methods?${query.toString()}`);
}
