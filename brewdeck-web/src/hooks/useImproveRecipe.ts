'use client';

import { useMutation } from '@tanstack/react-query';
import { improveRecipe } from '@/lib/api/ai';

export function useImproveRecipe() {
  return useMutation({
    mutationFn: (recipeId: number) => improveRecipe(recipeId),
  });
}
