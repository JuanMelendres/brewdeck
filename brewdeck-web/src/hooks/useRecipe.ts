'use client';

import { useQuery } from '@tanstack/react-query';
import { getRecipe, getRecipeStats } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useRecipe(id: number) {
  return useQuery({
    queryKey: keys.recipes.detail(id),
    queryFn: () => getRecipe(id),
  });
}

export function useRecipeStats(id: number) {
  return useQuery({
    queryKey: keys.recipes.stats(id),
    queryFn: () => getRecipeStats(id),
  });
}
