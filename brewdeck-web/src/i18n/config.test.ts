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
  it('serves English while Spanish is not enabled, whatever the cookie or browser asks for', () => {
    expect(resolveLocale('es', 'es-MX', ['en'])).toBe('en');
    expect(resolveLocale(undefined, 'es-MX,es;q=0.9', ['en'])).toBe('en');
  });

  it('serves Spanish once enabled, from the saved choice or the browser', () => {
    expect(resolveLocale('es', 'en-US', ['en', 'es'])).toBe('es');
    expect(resolveLocale(undefined, 'es-MX,es;q=0.9', ['en', 'es'])).toBe('es');
    expect(resolveLocale('en', 'es-MX', ['en', 'es'])).toBe('en');
  });

  it('falls back to English for an unknown saved value and no header', () => {
    expect(resolveLocale('xx', null, ['en', 'es'])).toBe('en');
  });
});
