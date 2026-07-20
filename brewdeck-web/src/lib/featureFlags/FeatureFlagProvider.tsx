'use client';

import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  DEFAULT_FEATURE_FLAGS,
  fetchFeatureFlags,
  type FeatureFlagName,
  type FeatureFlags,
} from '@/lib/api/featureFlags';
import { useAuth } from '@/lib/auth/AuthProvider';
import { keys } from '@/lib/query/keys';

type FeatureFlagStatus = 'loading' | 'ready' | 'error';

type FeatureFlagContextValue = {
  flags: FeatureFlags;
  status: FeatureFlagStatus;
};

const FeatureFlagContext = createContext<FeatureFlagContextValue | null>(null);

/**
 * Loads the current user's feature flags exactly once per session (TanStack Query dedupes by key)
 * and exposes them via context so no component fetches them independently. The backend remains the
 * source of truth — these values only drive what the UI shows; every protected action is still
 * validated server-side.
 *
 * <p>Fails safe: while loading or on error, every flag reads as disabled (see
 * {@link DEFAULT_FEATURE_FLAGS}), so unfinished functionality never flashes into view.
 */
export function FeatureFlagProvider({ children }: { children: ReactNode }) {
  const { user, status: authStatus } = useAuth();
  const enabled = authStatus === 'authenticated';

  const query = useQuery({
    queryKey: keys.featureFlags.forUser(user?.id ?? null),
    queryFn: fetchFeatureFlags,
    enabled,
    staleTime: Infinity, // Session-stable; a different user gets a different query key.
  });

  const value = useMemo<FeatureFlagContextValue>(() => {
    let status: FeatureFlagStatus;
    if (authStatus === 'anonymous') {
      status = 'ready'; // Nothing to gate; defaults (all disabled) apply.
    } else if (authStatus === 'loading' || query.isLoading) {
      status = 'loading';
    } else if (query.isError) {
      status = 'error';
    } else {
      status = 'ready';
    }
    return { flags: query.data ?? DEFAULT_FEATURE_FLAGS, status };
  }, [authStatus, query.isLoading, query.isError, query.data]);

  return <FeatureFlagContext.Provider value={value}>{children}</FeatureFlagContext.Provider>;
}

export function useFeatureFlags(): FeatureFlagContextValue {
  const context = useContext(FeatureFlagContext);
  if (!context) {
    throw new Error('useFeatureFlags must be used within a FeatureFlagProvider');
  }
  return context;
}

/** Convenience hook: whether a single feature is enabled for the current user. */
export function useFeatureFlag(name: FeatureFlagName): boolean {
  return useFeatureFlags().flags[name];
}
