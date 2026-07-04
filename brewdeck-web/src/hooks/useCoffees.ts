'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listCoffees, type ListCoffeesParams } from '@/lib/api/coffees';
import { keys } from '@/lib/query/keys';

export function useCoffees(params: ListCoffeesParams) {
  return useQuery({
    queryKey: keys.coffees.list(params),
    queryFn: () => listCoffees(params),
    placeholderData: keepPreviousData,
  });
}
