'use client';

import { useEffect, useRef } from 'react';
import { toAppLocale, useAppLanguage } from '@/i18n/useAppLanguage';
import { useAuth } from '@/lib/auth/AuthProvider';

/**
 * Applies the signed-in user's saved language, so it follows them across devices. Runs once per
 * mount: if the server still renders another language, it must not keep refreshing.
 */
export function LanguagePreferenceSync() {
  const { user } = useAuth();
  const { locale, setLocale } = useAppLanguage();
  const synced = useRef(false);
  const saved = user?.language ?? null;

  useEffect(() => {
    if (synced.current || !saved) {
      return;
    }
    synced.current = true;
    const wanted = toAppLocale(saved);
    if (wanted !== locale) {
      setLocale(wanted);
    }
  }, [saved, locale, setLocale]);

  return null;
}
