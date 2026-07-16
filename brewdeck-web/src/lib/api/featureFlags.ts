import { apiFetch } from './client';

/**
 * Client-facing feature flag aliases (camelCase). These mirror the backend `FrontendFeatureFlag`
 * registry one-to-one; the backend maps each alias to its kebab-case `feature_key`. Add a member
 * here only when the backend exposes the matching flag through `/api/feature-flags`.
 */
export type FeatureFlagName = 'aiRecipeAssistant';

export type FeatureFlags = Record<FeatureFlagName, boolean>;

type FeatureFlagsResponse = { features: Partial<Record<string, boolean>> };

/**
 * Fail-safe defaults: every known feature is disabled until the API confirms it is on. Used while
 * loading and on error so the UI never flashes unfinished functionality.
 */
export const DEFAULT_FEATURE_FLAGS: FeatureFlags = {
  aiRecipeAssistant: false,
};

/** Coerce a raw response map into a fully-populated, strongly-typed flag set. */
export function normalizeFeatureFlags(
  features: Partial<Record<string, boolean>> | undefined,
): FeatureFlags {
  return {
    aiRecipeAssistant: features?.aiRecipeAssistant ?? false,
  };
}

export async function fetchFeatureFlags(): Promise<FeatureFlags> {
  const response = await apiFetch<FeatureFlagsResponse>('/api/feature-flags');
  return normalizeFeatureFlags(response.features);
}
