/** Languages BrewDeck ships, the fallback, and the cookie that carries the choice to the server render. */
export const LOCALES = ['en', 'es'] as const;
export type AppLocale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: AppLocale = 'en';
export const LOCALE_COOKIE = 'brewdeck-locale';

export function isLocale(value: unknown): value is AppLocale {
  return typeof value === 'string' && (LOCALES as readonly string[]).includes(value);
}

/** The best supported language in an `Accept-Language` header ("es-MX,es;q=0.9,en;q=0.8"), else the default. */
export function localeFromAcceptLanguage(header: string | null | undefined): AppLocale {
  const ranked = (header ?? '')
    .split(',')
    .map((part) => {
      const [tag, ...params] = part.trim().split(';');
      const q = params.map((p) => p.trim()).find((p) => p.startsWith('q='));
      return { language: tag.split('-')[0].toLowerCase(), q: q ? Number(q.slice(2)) : 1 };
    })
    .filter((entry) => entry.language && !Number.isNaN(entry.q) && entry.q > 0)
    .sort((a, b) => b.q - a.q);
  return ranked.map((entry) => entry.language).find(isLocale) ?? DEFAULT_LOCALE;
}
