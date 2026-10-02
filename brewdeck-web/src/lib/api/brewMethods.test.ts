import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  createBrewMethod,
  deleteBrewMethod,
  listBrewMethods,
  listMethodUsage,
  updateBrewMethod,
} from './brewMethods';

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

describe('brew method write API', () => {
  it('createBrewMethod POSTs the body to /api/brew-methods', async () => {
    const fetchMock = stubFetch();
    await createBrewMethod({ name: 'Moka' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toMatch(/\/api\/brew-methods$/);
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ name: 'Moka' });
  });

  it('updateBrewMethod PUTs the body to /api/brew-methods/{id}', async () => {
    const fetchMock = stubFetch();
    await updateBrewMethod(3, { name: 'Bialetti' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/brew-methods/3');
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body)).toEqual({ name: 'Bialetti' });
  });

  it('deleteBrewMethod DELETEs /api/brew-methods/{id}', async () => {
    const fetchMock = stubFetch();
    await deleteBrewMethod(3);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/brew-methods/3');
    expect(init.method).toBe('DELETE');
  });
});
