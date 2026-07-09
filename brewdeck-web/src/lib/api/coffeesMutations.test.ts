import { afterEach, describe, expect, it, vi } from 'vitest';
import { createCoffee, updateCoffee, deleteCoffee } from './coffees';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ id: 1, name: 'Mezcla' }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('coffee write API', () => {
  it('createCoffee POSTs the body to /api/coffees', async () => {
    const fetchMock = stubFetch();
    await createCoffee({ name: 'Mezcla' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/coffees');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ name: 'Mezcla' });
  });

  it('updateCoffee PUTs the body to /api/coffees/{id}', async () => {
    const fetchMock = stubFetch();
    await updateCoffee(7, { name: 'Updated' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/coffees/7');
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body)).toEqual({ name: 'Updated' });
  });

  it('deleteCoffee DELETEs /api/coffees/{id}', async () => {
    const fetchMock = stubFetch();
    await deleteCoffee(7);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/coffees/7');
    expect(init.method).toBe('DELETE');
  });
});
