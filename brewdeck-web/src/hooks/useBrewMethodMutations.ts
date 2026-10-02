'use client';

import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { createBrewMethod, deleteBrewMethod, updateBrewMethod } from '@/lib/api/brewMethods';
import type { BrewMethodFormValues } from '@/lib/validation/brewMethodSchema';

// 'brew-methods' covers the list, usage analytics, and the recipe-form options;
// 'dashboard' holds the brew-method count.
function invalidateMethodQueries(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['brew-methods'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
  ]);
}

export function useCreateBrewMethod() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: BrewMethodFormValues) => createBrewMethod(body),
    onSuccess: () => invalidateMethodQueries(queryClient),
  });
}

export function useUpdateBrewMethod() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: BrewMethodFormValues }) =>
      updateBrewMethod(id, body),
    onSuccess: () => invalidateMethodQueries(queryClient),
  });
}

export function useDeleteBrewMethod() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteBrewMethod(id),
    onSuccess: () => invalidateMethodQueries(queryClient),
  });
}
