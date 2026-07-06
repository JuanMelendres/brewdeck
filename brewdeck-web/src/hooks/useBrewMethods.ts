'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listBrewMethods, type ListBrewMethodsParams } from '@/lib/api/brewMethods';
import { keys } from '@/lib/query/keys';

export function useBrewMethods(params: ListBrewMethodsParams) {
  return useQuery({
    queryKey: keys.brewMethods.list(params),
    queryFn: () => listBrewMethods(params),
    placeholderData: keepPreviousData,
  });
}
