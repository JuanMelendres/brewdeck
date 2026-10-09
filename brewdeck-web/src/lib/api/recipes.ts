import { apiFetch } from './client';
import type {
  MostBrewedRecipe,
  PageResponse,
  Recipe,
  RecipeFilters,
  RecipeStats,
  TopRatedRecipe,
} from './types';
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

export function listMostBrewedRecipes(limit = 5): Promise<MostBrewedRecipe[]> {
  const query = new URLSearchParams();
  query.set('limit', String(limit));

  return apiFetch<MostBrewedRecipe[]>(`/api/recipes/most-brewed?${query.toString()}`);
}

export function getRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}`);
}

export function getRecipeStats(id: number): Promise<RecipeStats> {
  return apiFetch<RecipeStats>(`/api/recipes/${id}/stats`);
}

/** Form values plus the AI provenance the form carries when a suggestion was used. */
export type RecipeRequestBody = RecipeFormValues & {
  aiModel?: string | null;
  aiPromptVersion?: string | null;
};

export function createRecipe(body: RecipeRequestBody): Promise<Recipe> {
  return apiFetch<Recipe>('/api/recipes', { method: 'POST', body: JSON.stringify(body) });
}

export function updateRecipe(id: number, body: RecipeRequestBody): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteRecipe(id: number): Promise<void> {
  return apiFetch<void>(`/api/recipes/${id}`, { method: 'DELETE' });
}

export function favoriteRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}/favorite`, { method: 'PATCH' });
}

export function unfavoriteRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}/unfavorite`, { method: 'PATCH' });
}

export function shareRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}/share`, { method: 'PATCH' });
}

export function unshareRecipe(id: number): Promise<Recipe> {
  return apiFetch<Recipe>(`/api/recipes/${id}/unshare`, { method: 'PATCH' });
}
