'use client';

import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import type { AppLocale } from '@/i18n/config';
import { toLanguage, useAppLanguage } from '@/i18n/useAppLanguage';
import { useAuth } from '@/lib/auth/AuthProvider';
import { LanguageChoice } from './LanguageChoice';

/** Language switch on the Account page; saves to the account, then re-renders in that language. */
export function LanguageSetting() {
  const t = useTranslations('language.setting');
  const { updateLanguage } = useAuth();
  const { locale, setLocale } = useAppLanguage();
  const [error, setError] = useState<string | null>(null);

  const onChange = async (next: AppLocale) => {
    setError(null);
    try {
      await updateLanguage(toLanguage(next));
    } catch {
      setError(t('saveFailed'));
    }
    // Applied either way: a failed save still switches this device.
    setLocale(next);
  };

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" component="h2">
        {t('title')}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {t('body')}
      </Typography>
      <LanguageChoice value={locale} onChange={onChange} />
      {error ? <Alert severity="error">{error}</Alert> : null}
    </Stack>
  );
}
