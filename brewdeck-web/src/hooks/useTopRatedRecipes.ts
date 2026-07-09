'use client';

import { useQuery } from '@tanstack/react-query';
import { listTopRatedRecipes } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useTopRatedRecipes(limit = 5) {
  return useQuery({
    queryKey: keys.recipes.topRated(limit),
    queryFn: () => listTopRatedRecipes(limit),
  });
}
