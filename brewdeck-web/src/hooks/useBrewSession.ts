'use client';

import { useQuery } from '@tanstack/react-query';
import { getBrewSession } from '@/lib/api/brewSessions';
import { keys } from '@/lib/query/keys';

export function useBrewSession(id: number) {
  return useQuery({
    queryKey: keys.brewSessions.detail(id),
    queryFn: () => getBrewSession(id),
  });
}
