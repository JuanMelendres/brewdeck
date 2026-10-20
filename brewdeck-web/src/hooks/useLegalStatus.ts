'use client';

import { useQuery } from '@tanstack/react-query';
import { getLegalStatus } from '@/lib/api/legal';
import { keys } from '@/lib/query/keys';

/**
 * The legal texts' status (LEGAL-01). Treat anything but a confirmed `published: true` as not
 * published, so a failed request never shows draft texts or asks for consent.
 */
export function useLegalStatus() {
  const query = useQuery({
    queryKey: keys.legal.status,
    queryFn: getLegalStatus,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  return {
    isLoading: query.isLoading,
    published: query.data?.published === true,
    termsVersion: query.data?.termsVersion ?? null,
  };
}
