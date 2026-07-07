'use client';

import { useQuery } from '@tanstack/react-query';
import { getPublicRecipe } from '@/lib/api/publicRecipes';
import { keys } from '@/lib/query/keys';

export function usePublicRecipe(token: string) {
  return useQuery({
    queryKey: keys.recipes.public(token),
    queryFn: () => getPublicRecipe(token),
    retry: false,
  });
}
