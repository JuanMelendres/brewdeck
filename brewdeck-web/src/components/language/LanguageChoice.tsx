'use client';

import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import { useTranslations } from 'next-intl';
import { LOCALES, type AppLocale } from '@/i18n/config';

/** The English/Spanish pair shared by the Account setting and the first-login dialog. */
export function LanguageChoice({
  value,
  onChange,
  disabled,
  fullWidth,
}: {
  value: AppLocale;
  onChange: (locale: AppLocale) => void;
  disabled?: boolean;
  fullWidth?: boolean;
}) {
  const t = useTranslations('language');
  return (
    <ToggleButtonGroup
      exclusive
      value={value}
      onChange={(_event, next: AppLocale | null) => {
        if (next) onChange(next);
      }}
      aria-label={t('group')}
      disabled={disabled}
      fullWidth={fullWidth}
      sx={{ alignSelf: fullWidth ? 'stretch' : 'flex-start' }}
    >
      {LOCALES.map((locale) => (
        // Each language is named in itself ("Español"), so people find theirs whatever the UI shows.
        <ToggleButton key={locale} value={locale} lang={locale} sx={{ px: 2.5, py: 1.25, textTransform: 'none' }}>
          {t(locale)}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
}
