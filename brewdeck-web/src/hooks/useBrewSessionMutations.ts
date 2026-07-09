'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createBrewSession } from '@/lib/api/brewSessions';
import type { BrewSessionFormValues } from '@/lib/validation/brewSessionSchema';

export function useCreateBrewSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: BrewSessionFormValues) => createBrewSession(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['brew-sessions'] });
      queryClient.invalidateQueries({ queryKey: ['recipes'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}
