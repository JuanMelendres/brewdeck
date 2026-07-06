import { afterEach, describe, expect, it, vi } from 'vitest';
import { listCoffees, listMostUsedCoffees } from './coffees';

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

describe('listCoffees', () => {
  it('includes page, size and default sort, and omits blank filters', async () => {
    const fetchMock = stubFetch();

    await listCoffees({ page: 2, size: 20, filters: { name: '', origin: 'Veracruz' } });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/coffees?');
    expect(url).toContain('page=2');
    expect(url).toContain('size=20');
    expect(url).toContain('sort=id%2Casc');
    expect(url).toContain('origin=Veracruz');
    expect(url).not.toContain('name=');
  });

  it('includes all non-blank filters and a custom sort', async () => {
    const fetchMock = stubFetch();

    await listCoffees({
      page: 0,
      size: 10,
      sort: 'name,asc',
      filters: { name: 'Blend', origin: 'Oaxaca', roastLevel: 'Medio', process: 'Lavado' },
    });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('sort=name%2Casc');
    expect(url).toContain('name=Blend');
    expect(url).toContain('origin=Oaxaca');
    expect(url).toContain('roastLevel=Medio');
    expect(url).toContain('process=Lavado');
  });
});

describe('listMostUsedCoffees', () => {
  it('requests /api/coffees/most-used with the limit (default 5)', async () => {
    const fetchMock = stubFetch();

    await listMostUsedCoffees();

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/coffees/most-used?');
    expect(url).toContain('limit=5');
  });

  it('forwards a custom limit', async () => {
    const fetchMock = stubFetch();

    await listMostUsedCoffees(10);

    expect(String(fetchMock.mock.calls[0][0])).toContain('limit=10');
  });
});
