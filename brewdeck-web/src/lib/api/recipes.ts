import { apiFetch } from './client';
import type { PageResponse, Recipe, RecipeFilters, RecipeStats, TopRatedRecipe } from './types';
import type { RecipeFormValues } from '@/lib/validation/recipeSchema';

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

export type ListFavoriteRecipesParams = { page: number; size: number; sort?: string };

export function listFavoriteRecipes(
  params: ListFavoriteRecipesParams,
): Promise<PageResponse<Recipe>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  return apiFetch<PageResponse<Recipe>>(`/api/recipes/favorites?${query.toString()}`);
}

export function listTopRatedRecipes(limit = 5): Promise<TopRatedRecipe[]> {
  const query = new URLSearchParams();
  query.set('limit', String(limit));

  return apiFetch<TopRatedRecipe[]>(`/api/recipes/top-rated?${query.toString()}`);
}

export function getRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}`);
}

export function getRecipeStats(id: number): Promise<RecipeStats> {
  return apiFetch<RecipeStats>(`/api/recipes/${id}/stats`);
}

export function createRecipe(body: RecipeFormValues): Promise<Recipe> {
  return apiFetch<Recipe>('/api/recipes', { method: 'POST', body: JSON.stringify(body) });
}

export function updateRecipe(id: number, body: RecipeFormValues): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteRecipe(id: number): Promise<void> {
  return apiFetch<void>(`/api/recipes/${id}`, { method: 'DELETE' });
}
