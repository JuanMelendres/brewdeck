import { apiFetch } from './client';
import type { PublicRecipe } from './types';

export function getPublicRecipe(token: string): Promise<PublicRecipe> {
  return apiFetch<PublicRecipe>(`/api/public/recipes/${encodeURIComponent(token)}`);
}
