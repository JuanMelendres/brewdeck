'use client';

import { useQuery } from '@tanstack/react-query';
import { listMostUsedCoffees } from '@/lib/api/coffees';
import { keys } from '@/lib/query/keys';

export function useMostUsedCoffees(limit = 5) {
  return useQuery({
    queryKey: keys.coffees.mostUsed(limit),
    queryFn: () => listMostUsedCoffees(limit),
  });
}
