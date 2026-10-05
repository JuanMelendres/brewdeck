'use client';

import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import { useColorScheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { useAuth } from '@/lib/auth/AuthProvider';
import { toThemePreference, type ThemeMode } from '@/lib/theme/themePreference';
import { ThemeChoice } from './ThemeChoice';

/** Light/dark switch on the Account page; applies at once and saves to the account. */
export function ThemeModeSetting() {
  const t = useTranslations('theme.setting');
  const { updateTheme } = useAuth();
  const { mode, systemMode, setMode } = useColorScheme();
  const [error, setError] = useState<string | null>(null);
  // `mode` is undefined until the provider mounts; "system" resolves to the OS mode.
  const current: ThemeMode = (mode === 'system' ? systemMode : mode) ?? 'light';

  const onChange = async (next: ThemeMode) => {
    setError(null);
    setMode(next);
    try {
      await updateTheme(toThemePreference(next));
    } catch {
      setError(t('saveFailed'));
    }
  };

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" component="h2">
        {t('title')}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {t('body')}
      </Typography>
      <ThemeChoice value={current} onChange={onChange} />
      {error ? <Alert severity="error">{error}</Alert> : null}
    </Stack>
  );
}
