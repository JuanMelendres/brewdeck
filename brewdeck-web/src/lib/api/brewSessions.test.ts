import { afterEach, describe, expect, it, vi } from 'vitest';
import { listBrewSessions } from './brewSessions';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('listBrewSessions', () => {
  it('includes page/size/default sort and omits an unset rating', async () => {
    const fetchMock = stubFetch();

    await listBrewSessions({ page: 2, size: 20 });

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/brew-sessions?');
    expect(url).toContain('page=2');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=id%2Casc');
    expect(url).not.toContain('rating=');
  });

  it('includes a rating and a custom sort when provided', async () => {
    const fetchMock = stubFetch();

    await listBrewSessions({ page: 0, size: 10, sort: 'brewedAt,desc', filters: { rating: 9 } });

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('sort=brewedAt%2Cdesc');
    expect(url).toContain('rating=9');
  });
});
