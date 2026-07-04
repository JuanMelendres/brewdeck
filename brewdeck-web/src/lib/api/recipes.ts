import { apiFetch } from './client';
import type { PageResponse, Recipe, RecipeFilters } from './types';

export type ListRecipesParams = {
  page: number;
  size: number;
  sort?: string;
  filters?: RecipeFilters;
};

export function listRecipes(params: ListRecipesParams): Promise<PageResponse<Recipe>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  const filters = params.filters ?? {};
  const name = filters.name?.trim();
  if (name) {
    query.set('name', name);
  }
  if (filters.favorite === true) {
    query.set('favorite', 'true');
  }

  return apiFetch<PageResponse<Recipe>>(`/api/recipes?${query.toString()}`);
}
