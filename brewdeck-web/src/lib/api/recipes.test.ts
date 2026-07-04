import { afterEach, describe, expect, it, vi } from 'vitest';
import { listRecipes } from './recipes';

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

describe('listRecipes', () => {
  it('includes page/size/default sort and omits a blank name and unset favorite', async () => {
    const fetchMock = stubFetch();

    await listRecipes({ page: 1, size: 20, filters: { name: '  ' } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/recipes?');
    expect(url).toContain('page=1');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=id%2Casc');
    expect(url).not.toContain('name=');
    expect(url).not.toContain('favorite=');
  });

  it('includes a non-blank name, favorite=true, and a custom sort', async () => {
    const fetchMock = stubFetch();

    await listRecipes({ page: 0, size: 10, sort: 'name,asc', filters: { name: 'AeroPress', favorite: true } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('sort=name%2Casc');
    expect(url).toContain('name=AeroPress');
    expect(url).toContain('favorite=true');
  });

  it('omits favorite when it is false', async () => {
    const fetchMock = stubFetch();

    await listRecipes({ page: 0, size: 10, filters: { favorite: false } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).not.toContain('favorite=');
  });
});
