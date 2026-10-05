'use client';

import { useTranslations } from 'next-intl';

/**
 * Translates a form error. Zod schemas use keys under `validation` as their messages; server
 * messages (already translated by the API) and anything unknown pass through unchanged.
 */
export function useFieldError() {
  const t = useTranslations('validation');
  return (message: string | undefined): string | undefined => {
    if (message === undefined) {
      return undefined;
    }
    return t.has(message as Parameters<typeof t>[0]) ? t(message as Parameters<typeof t>[0]) : message;
  };
}
