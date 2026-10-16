'use client';

import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import {
  createBrewSession,
  deleteBrewSession,
  updateBrewSession,
} from '@/lib/api/brewSessions';
import { keys } from '@/lib/query/keys';
import type { BrewSessionFormValues } from '@/lib/validation/brewSessionSchema';

/** A session change moves session lists, recipe stats and rankings, and the dashboard. */
function invalidateAfterSessionChange(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: ['brew-sessions'] });
  queryClient.invalidateQueries({ queryKey: ['recipes'] });
  queryClient.invalidateQueries({ queryKey: ['dashboard'] });
}

export function useCreateBrewSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: BrewSessionFormValues) => createBrewSession(body),
    onSuccess: () => invalidateAfterSessionChange(queryClient),
  });
}

export function useUpdateBrewSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: BrewSessionFormValues }) =>
      updateBrewSession(id, body),
    onSuccess: () => invalidateAfterSessionChange(queryClient),
  });
}

export function useDeleteBrewSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteBrewSession(id),
    onSuccess: (_data, id) => {
      // The detail page is about to navigate away; drop its entry instead of refetching a 404.
      queryClient.removeQueries({ queryKey: keys.brewSessions.detail(id) });
      invalidateAfterSessionChange(queryClient);
    },
  });
}
