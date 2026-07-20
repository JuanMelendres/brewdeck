'use client';

import { useEffect, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useFeatureFlags } from '@/lib/featureFlags/FeatureFlagProvider';
import { Spinner } from '@/components/ui/Spinner';
import type { FeatureFlagName } from '@/lib/api/featureFlags';

type FeatureRouteGuardProps = {
  name: FeatureFlagName;
  children: ReactNode;
  /** Where to send the user when the feature is disabled. */
  redirectTo?: string;
};

/**
 * Route-level guard for pages behind a feature flag. Handles direct URL access to a disabled
 * feature: while flags load it shows a spinner (no flash of the page), and once known-disabled it
 * redirects. The backend still enforces the flag on every request the page would make.
 */
export function FeatureRouteGuard({
  name,
  children,
  redirectTo = '/dashboard',
}: FeatureRouteGuardProps) {
  const { flags, status } = useFeatureFlags();
  const router = useRouter();
  const enabled = flags[name];

  useEffect(() => {
    if (status === 'ready' && !enabled) {
      router.replace(redirectTo);
    }
  }, [status, enabled, redirectTo, router]);

  if (status === 'loading') {
    return <Spinner />;
  }
  if (!enabled) {
    return null; // Disabled (or errored → fail safe): render nothing while redirecting.
  }
  return <>{children}</>;
}
