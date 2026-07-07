import { apiFetch } from './client';

export type SuggestRecipeInput = {
  coffeeId: number;
  methodId: number;
  notes?: string;
};

export type SuggestedRecipe = {
  coffeeGrams: number | null;
  waterGrams: number | null;
  ratio: string | null;
  grindSetting: string | null;
  waterTemp: number | null;
  brewTime: string | null;
  steps: string | null;
  rationale: string;
};

export function suggestRecipe(body: SuggestRecipeInput): Promise<SuggestedRecipe> {
  return apiFetch<SuggestedRecipe>('/api/recipes/suggest', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
