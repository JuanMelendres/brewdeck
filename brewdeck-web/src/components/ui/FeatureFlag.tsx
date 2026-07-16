'use client';

import type { ReactNode } from 'react';
import { useFeatureFlag } from '@/lib/featureFlags/FeatureFlagProvider';
import type { FeatureFlagName } from '@/lib/api/featureFlags';

type FeatureFlagProps = {
  name: FeatureFlagName;
  children: ReactNode;
  /** Rendered when the feature is disabled. Defaults to nothing. */
  fallback?: ReactNode;
};

/**
 * Renders {@link children} only when the named feature is enabled for the current user, otherwise
 * {@link fallback}. This is presentation only — never rely on it as a security boundary; the backend
 * validates every protected action independently.
 */
export function FeatureFlag({ name, children, fallback = null }: FeatureFlagProps) {
  const enabled = useFeatureFlag(name);
  return <>{enabled ? children : fallback}</>;
}
