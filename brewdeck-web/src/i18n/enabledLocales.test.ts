import { afterEach, describe, expect, it, vi } from 'vitest';
import { enabledLocales } from './enabledLocales';

function respond(body: unknown, ok = true) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok, json: () => Promise.resolve(body) }));
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('enabledLocales', () => {
  it('returns the languages the backend has enabled', async () => {
    respond({ languages: ['en', 'es'] });

    expect(await enabledLocales()).toEqual(['en', 'es']);
  });

  it('caches the backend answer on the Next server for a minute', async () => {
    respond({ languages: ['en'] });

    await enabledLocales();

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/public/ui-config'),
      expect.objectContaining({ next: { revalidate: 60 } }),
    );
  });

  it('ignores unknown languages and always keeps English', async () => {
    respond({ languages: ['es', 'fr'] });

    expect(await enabledLocales()).toEqual(['en', 'es']);
  });

  it.each([
    ['an error response', () => respond({}, false)],
    ['a malformed body', () => respond({ languages: 'es' })],
    ['a network failure', () => vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('down')))],
  ])('falls back to English only on %s', async (_, arrange) => {
    arrange();

    expect(await enabledLocales()).toEqual(['en']);
  });
});
