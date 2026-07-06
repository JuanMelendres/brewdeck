'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listFavoriteRecipes, type ListFavoriteRecipesParams } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useFavoriteRecipes(params: ListFavoriteRecipesParams) {
  return useQuery({
    queryKey: keys.recipes.favorites(params),
    queryFn: () => listFavoriteRecipes(params),
    placeholderData: keepPreviousData,
  });
}
