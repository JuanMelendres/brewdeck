import { describe, expect, it } from 'vitest';
import { localeFromAcceptLanguage, resolveLocale } from './config';

describe('localeFromAcceptLanguage', () => {
  it.each([
    ['es-MX,es;q=0.9,en;q=0.8', 'es'],
    ['en-US,en;q=0.9', 'en'],
    ['fr-FR,fr;q=0.9,es;q=0.5', 'es'],
    ['en;q=0.2,es;q=0.8', 'es'],
    ['fr-FR', 'en'],
    ['', 'en'],
  ])('picks the best supported language from "%s"', (header, expected) => {
    expect(localeFromAcceptLanguage(header)).toBe(expected);
  });

  it('falls back to English without a header', () => {
    expect(localeFromAcceptLanguage(null)).toBe('en');
  });
});

describe('resolveLocale', () => {
  it('prefers the saved choice, then the browser language', () => {
    expect(resolveLocale('es', 'en-US')).toBe('es');
    expect(resolveLocale(undefined, 'es-MX,es;q=0.9')).toBe('es');
    expect(resolveLocale('en', 'es-MX')).toBe('en');
  });

  it('falls back to English for an unknown saved value and no header', () => {
    expect(resolveLocale('xx', null)).toBe('en');
  });
});
