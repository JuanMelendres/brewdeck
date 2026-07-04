'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listRecipes, type ListRecipesParams } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useRecipes(params: ListRecipesParams) {
  return useQuery({
    queryKey: keys.recipes.list(params),
    queryFn: () => listRecipes(params),
    placeholderData: keepPreviousData,
  });
}
