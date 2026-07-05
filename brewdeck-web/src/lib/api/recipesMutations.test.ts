import { afterEach, describe, expect, it, vi } from 'vitest';
import { createRecipe, updateRecipe, deleteRecipe } from './recipes';

function stubFetch() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ id: 1, name: 'AeroPress' }),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('recipe write API', () => {
  it('createRecipe POSTs to /api/recipes', async () => {
    const fetchMock = stubFetch();
    await createRecipe({ coffeeId: 1, methodId: 2, name: 'AeroPress' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/recipes');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ coffeeId: 1, methodId: 2, name: 'AeroPress' });
  });

  it('updateRecipe PUTs to /api/recipes/{id}', async () => {
    const fetchMock = stubFetch();
    await updateRecipe(7, { coffeeId: 1, methodId: 2, name: 'Updated' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/recipes/7');
    expect(init.method).toBe('PUT');
  });

  it('deleteRecipe DELETEs /api/recipes/{id}', async () => {
    const fetchMock = stubFetch();
    await deleteRecipe(7);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/recipes/7');
    expect(init.method).toBe('DELETE');
  });
});
