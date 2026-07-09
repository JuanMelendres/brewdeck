import { afterEach, describe, expect, it, vi } from 'vitest';
import { listBrewMethods, listMethodUsage } from './brewMethods';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0, first: true, last: true }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('listBrewMethods', () => {
  it('requests /api/brew-methods with page and size', async () => {
    const fetchMock = stubFetch();
    await listBrewMethods({ page: 0, size: 100 });
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/brew-methods?');
    expect(url).toContain('page=0');
    expect(url).toContain('size=100');
  });
});

describe('listMethodUsage', () => {
  it('requests /api/brew-methods/usage', async () => {
    const fetchMock = stubFetch();
    await listMethodUsage();
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/brew-methods/usage');
  });
});
