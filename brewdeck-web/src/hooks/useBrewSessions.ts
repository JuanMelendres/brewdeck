'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listBrewSessions, type ListBrewSessionsParams } from '@/lib/api/brewSessions';
import { keys } from '@/lib/query/keys';

export function useBrewSessions(params: ListBrewSessionsParams) {
  return useQuery({
    queryKey: keys.brewSessions.list(params),
    queryFn: () => listBrewSessions(params),
    placeholderData: keepPreviousData,
  });
}
