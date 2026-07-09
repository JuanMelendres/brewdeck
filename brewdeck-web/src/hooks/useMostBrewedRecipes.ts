'use client';

import { useQuery } from '@tanstack/react-query';
import { listMostBrewedRecipes } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useMostBrewedRecipes(limit = 5) {
  return useQuery({
    queryKey: keys.recipes.mostBrewed(limit),
    queryFn: () => listMostBrewedRecipes(limit),
  });
}
