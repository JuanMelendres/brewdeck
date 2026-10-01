import InitColorSchemeScript from '@mui/material/InitColorSchemeScript';
import type { Metadata } from 'next';
import { DM_Sans, Fraunces } from 'next/font/google';
import type { ReactNode } from 'react';
import { THEME_MODE_STORAGE_KEY } from '@/lib/theme/theme';
import { Providers } from './providers';

const bodyFont = DM_Sans({ subsets: ['latin'], variable: '--font-body', display: 'swap' });
const displayFont = Fraunces({ subsets: ['latin'], variable: '--font-display', display: 'swap' });

export const metadata: Metadata = {
  title: 'BrewDeck',
  description: 'Coffee brewing companion',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    // The color-scheme script sets a class on <html> before hydration, so React must not flag it.
    <html lang="en" className={`${bodyFont.variable} ${displayFont.variable}`} suppressHydrationWarning>
      <body>
        <InitColorSchemeScript
          attribute="class"
          defaultMode="light"
          modeStorageKey={THEME_MODE_STORAGE_KEY}
        />
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
