'use client';

import { useQuery } from '@tanstack/react-query';
import { getCoffee } from '@/lib/api/coffees';
import { keys } from '@/lib/query/keys';

export function useCoffee(id: number) {
  return useQuery({
    queryKey: keys.coffees.detail(id),
    queryFn: () => getCoffee(id),
  });
}
