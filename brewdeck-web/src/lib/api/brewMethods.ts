import { apiFetch } from './client';
import type { PageResponse } from './types';

export type BrewMethod = {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string | null;
};

export type ListBrewMethodsParams = { page: number; size: number };

export function listBrewMethods(params: ListBrewMethodsParams): Promise<PageResponse<BrewMethod>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  return apiFetch<PageResponse<BrewMethod>>(`/api/brew-methods?${query.toString()}`);
}

export type MethodUsage = {
  methodId: number;
  methodName: string;
  recipeCount: number;
};

export function listMethodUsage(): Promise<MethodUsage[]> {
  return apiFetch<MethodUsage[]>('/api/brew-methods/usage');
}
