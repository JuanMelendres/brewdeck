'use client';

import { useQuery } from '@tanstack/react-query';
import { listBrewSessionsByRecipe } from '@/lib/api/brewSessions';
import { keys } from '@/lib/query/keys';

export function useRecipeBrewSessions(recipeId: number) {
  return useQuery({
    queryKey: keys.brewSessions.byRecipe(recipeId),
    queryFn: () => listBrewSessionsByRecipe(recipeId, { page: 0, size: 50 }),
  });
}
