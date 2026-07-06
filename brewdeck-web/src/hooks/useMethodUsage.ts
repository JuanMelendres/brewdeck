'use client';

import { useQuery } from '@tanstack/react-query';
import { listMethodUsage } from '@/lib/api/brewMethods';
import { keys } from '@/lib/query/keys';

export function useMethodUsage() {
  return useQuery({
    queryKey: keys.brewMethods.usage,
    queryFn: listMethodUsage,
  });
}
