import { render } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import { NextIntlClientProvider } from 'next-intl';
import type { ReactElement, ReactNode } from 'react';
import en from '../../messages/en.json';
import es from '../../messages/es.json';
import type { AppLocale } from '@/i18n/config';
import { THEME_MODE_STORAGE_KEY, theme } from '@/lib/theme/theme';

const MESSAGES = { en, es } as const;

/** Renders with the app theme and translations (English unless a test asks for another locale). */
export function renderWithTheme(ui: ReactElement, { locale = 'en' }: { locale?: AppLocale } = {}) {
  // A wrapper (not a wrapped element), so `rerender` keeps the providers too.
  function Providers({ children }: { children: ReactNode }) {
    return (
      <NextIntlClientProvider locale={locale} messages={MESSAGES[locale]}>
        <ThemeProvider theme={theme} defaultMode="light" modeStorageKey={THEME_MODE_STORAGE_KEY}>
          {children}
        </ThemeProvider>
      </NextIntlClientProvider>
    );
  }
  return render(ui, { wrapper: Providers });
}
