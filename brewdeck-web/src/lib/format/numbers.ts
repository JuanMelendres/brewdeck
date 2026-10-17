import { DEFAULT_LOCALE, type AppLocale } from '@/i18n/config';

const DASH = '—';

/**
 * An average rating with exactly one decimal in the app's language: "8.4" in English, "8,4" in
 * Spanish. The API already rounds averages half up (DATA-01); this only localises the separator.
 */
export function formatAverage(value: number | null, locale: AppLocale = DEFAULT_LOCALE): string {
  if (value === null || Number.isNaN(value)) {
    return DASH;
  }
  return new Intl.NumberFormat(locale, { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(
    value,
  );
}
