'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { shareRecipe, unshareRecipe } from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useShareRecipe(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => shareRecipe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.recipes.detail(id) }),
  });
}

export function useUnshareRecipe(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => unshareRecipe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.recipes.detail(id) }),
  });
}
