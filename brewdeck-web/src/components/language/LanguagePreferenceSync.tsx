'use client';

import { useEffect, useRef } from 'react';
import { toAppLocale, useAppLanguage } from '@/i18n/useAppLanguage';
import { useAuth } from '@/lib/auth/AuthProvider';
import { useFeatureFlag } from '@/lib/featureFlags/FeatureFlagProvider';

/**
 * Applies the signed-in user's saved language, so it follows them across devices. Runs once per
 * mount: if the server still renders another language (e.g. Spanish was just turned off), it must
 * not keep refreshing.
 */
export function LanguagePreferenceSync() {
  const { user } = useAuth();
  const spanishEnabled = useFeatureFlag('i18nSpanish');
  const { locale, setLocale } = useAppLanguage();
  const synced = useRef(false);
  const saved = user?.language ?? null;

  useEffect(() => {
    if (synced.current || !saved || !spanishEnabled) {
      return;
    }
    synced.current = true;
    const wanted = toAppLocale(saved);
    if (wanted !== locale) {
      setLocale(wanted);
    }
  }, [saved, spanishEnabled, locale, setLocale]);

  return null;
}
