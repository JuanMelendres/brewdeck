'use client';

import { useMutation } from '@tanstack/react-query';
import { suggestRecipe, type SuggestRecipeInput } from '@/lib/api/ai';

export function useSuggestRecipe() {
  return useMutation({
    mutationFn: (body: SuggestRecipeInput) => suggestRecipe(body),
  });
}
