'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createRecipe,
  deleteRecipe,
  favoriteRecipe,
  type RecipeRequestBody,
  unfavoriteRecipe,
  updateRecipe,
} from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

export function useCreateRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: RecipeRequestBody) => createRecipe(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  });
}

export function useUpdateRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: RecipeRequestBody }) => updateRecipe(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  });
}

export function useDeleteRecipe() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteRecipe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  });
}

/** Marks or unmarks a recipe as favorite; refreshes recipe lists and the dashboard's favorite count. */
export function useToggleFavorite() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, favorite }: { id: number; favorite: boolean }) =>
      favorite ? favoriteRecipe(id) : unfavoriteRecipe(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['recipes'] });
      void queryClient.invalidateQueries({ queryKey: keys.dashboard.summary });
    },
  });
}
