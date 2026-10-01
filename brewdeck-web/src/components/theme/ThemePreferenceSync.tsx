'use client';

import { useColorScheme } from '@mui/material/styles';
import { useEffect } from 'react';
import { useAuth } from '@/lib/auth/AuthProvider';
import { toThemeMode } from '@/lib/theme/themePreference';

/**
 * Applies the signed-in user's saved theme, so it follows them across browsers. The browser copy
 * (MUI's stored mode) keeps rendering the last theme until `/me` arrives.
 */
export function ThemePreferenceSync() {
  const { user } = useAuth();
  const { setMode } = useColorScheme();
  const preference = user?.themePreference ?? null;

  useEffect(() => {
    if (preference) {
      setMode(toThemeMode(preference));
    }
  }, [preference, setMode]);

  return null;
}
