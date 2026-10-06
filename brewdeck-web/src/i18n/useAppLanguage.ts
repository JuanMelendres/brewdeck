'use client';

import { useLocale } from 'next-intl';
import { useRouter } from 'next/navigation';
import { useCallback } from 'react';
import type { Language } from '@/lib/api/types';
import { LOCALE_COOKIE, type AppLocale } from './config';

const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

export function toAppLocale(language: Language): AppLocale {
  return language === 'ES' ? 'es' : 'en';
}

export function toLanguage(locale: AppLocale): Language {
  return locale === 'es' ? 'ES' : 'EN';
}

/** Saves the device's language for the server render (src/i18n/request.ts). */
export function writeLocaleCookie(locale: AppLocale): void {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=${ONE_YEAR_SECONDS}; samesite=lax`;
}

/**
 * The active UI language and a way to switch it: writes the cookie, then re-renders on the server
 * so the layout and messages come back in the new language without a full reload.
 */
export function useAppLanguage(): { locale: AppLocale; setLocale: (locale: AppLocale) => void } {
  const locale = useLocale();
  const router = useRouter();
  const setLocale = useCallback(
    (next: AppLocale) => {
      writeLocaleCookie(next);
      router.refresh();
    },
    [router],
  );
  return { locale, setLocale };
}
