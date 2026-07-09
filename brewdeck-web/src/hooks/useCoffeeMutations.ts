'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createCoffee, deleteCoffee, updateCoffee } from '@/lib/api/coffees';
import type { CoffeeFormValues } from '@/lib/validation/coffeeSchema';

export function useCreateCoffee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CoffeeFormValues) => createCoffee(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['coffees'] }),
  });
}

export function useUpdateCoffee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: CoffeeFormValues }) => updateCoffee(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['coffees'] }),
  });
}

export function useDeleteCoffee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteCoffee(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['coffees'] }),
  });
}
