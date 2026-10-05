import InitColorSchemeScript from '@mui/material/InitColorSchemeScript';
import type { Metadata } from 'next';
import { NextIntlClientProvider } from 'next-intl';
import { getLocale, getTranslations } from 'next-intl/server';
import { DM_Sans, Fraunces } from 'next/font/google';
import type { ReactNode } from 'react';
import { THEME_MODE_STORAGE_KEY } from '@/lib/theme/theme';
import { Providers } from './providers';

const bodyFont = DM_Sans({ subsets: ['latin'], variable: '--font-body', display: 'swap' });
const displayFont = Fraunces({ subsets: ['latin'], variable: '--font-display', display: 'swap' });

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('meta');
  return { title: 'BrewDeck', description: t('description') };
}

export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getLocale();
  return (
    // The color-scheme script sets a class on <html> before hydration, so React must not flag it.
    <html lang={locale} className={`${bodyFont.variable} ${displayFont.variable}`} suppressHydrationWarning>
      <body>
        <InitColorSchemeScript
          attribute="class"
          defaultMode="light"
          modeStorageKey={THEME_MODE_STORAGE_KEY}
        />
        <NextIntlClientProvider>
          <Providers>{children}</Providers>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
