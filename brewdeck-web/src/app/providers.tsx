'use client';

import { AppRouterCacheProvider } from '@mui/material-nextjs/v16-appRouter';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import type { ReactNode } from 'react';
import { THEME_MODE_STORAGE_KEY, theme } from '@/lib/theme/theme';
import { AuthProvider } from '@/lib/auth/AuthProvider';
import { FeatureFlagProvider } from '@/lib/featureFlags/FeatureFlagProvider';
import { QueryProvider } from '@/lib/query/provider';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AppRouterCacheProvider>
      <ThemeProvider theme={theme} defaultMode="light" modeStorageKey={THEME_MODE_STORAGE_KEY}>
        <CssBaseline />
        <QueryProvider>
          <AuthProvider>
            <FeatureFlagProvider>{children}</FeatureFlagProvider>
          </AuthProvider>
        </QueryProvider>
      </ThemeProvider>
    </AppRouterCacheProvider>
  );
}
