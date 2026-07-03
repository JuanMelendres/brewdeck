'use client';

import { useQuery } from '@tanstack/react-query';
import { getDashboardSummary } from '@/lib/api/dashboard';
import { keys } from '@/lib/query/keys';

export function useDashboardSummary() {
  return useQuery({
    queryKey: keys.dashboard.summary,
    queryFn: getDashboardSummary,
  });
}
