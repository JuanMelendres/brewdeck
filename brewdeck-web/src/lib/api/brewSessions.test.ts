import { afterEach, describe, expect, it, vi } from 'vitest';
import { createBrewSession, listBrewSessions, listBrewSessionsByRecipe } from './brewSessions';

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

describe('createBrewSession', () => {
  it('POSTs to /api/brew-sessions with the body', async () => {
    const fetchMock = stubFetch();

    await createBrewSession({ recipeId: 1, rating: 9 });

    const url = String(fetchMock.mock.calls[0][0]);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(url).toContain('/api/brew-sessions');
    expect(init.method).toBe('POST');
    expect(String(init.body)).toContain('"recipeId":1');
  });
});

describe('listBrewSessionsByRecipe', () => {
  it('requests the recipe sessions with a brewedAt,desc default sort', async () => {
    const fetchMock = stubFetch();

    await listBrewSessionsByRecipe(7, { page: 0, size: 50 });

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/brew-sessions/recipe/7?');
    expect(url).toContain('page=0');
    expect(url).toContain('size=50');
    expect(url).toContain('sort=brewedAt%2Cdesc');
  });
});
