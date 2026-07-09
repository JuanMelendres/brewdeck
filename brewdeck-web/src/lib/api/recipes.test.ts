import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  getRecipe,
  getRecipeStats,
  listFavoriteRecipes,
  listMostBrewedRecipes,
  listRecipes,
  listTopRatedRecipes,
} from './recipes';

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

describe('listTopRatedRecipes', () => {
  it('requests /api/recipes/top-rated with the limit (default 5)', async () => {
    const fetchMock = stubFetch();

    await listTopRatedRecipes();

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/recipes/top-rated?');
    expect(url).toContain('limit=5');
  });

  it('forwards a custom limit', async () => {
    const fetchMock = stubFetch();

    await listTopRatedRecipes(10);

    expect(String(fetchMock.mock.calls[0][0])).toContain('limit=10');
  });
});

describe('listMostBrewedRecipes', () => {
  it('requests /api/recipes/most-brewed with the limit (default 5)', async () => {
    const fetchMock = stubFetch();

    await listMostBrewedRecipes();

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain('/api/recipes/most-brewed?');
    expect(url).toContain('limit=5');
  });

  it('forwards a custom limit', async () => {
    const fetchMock = stubFetch();

    await listMostBrewedRecipes(10);

    expect(String(fetchMock.mock.calls[0][0])).toContain('limit=10');
  });
});

describe('listFavoriteRecipes', () => {
  it('requests /api/recipes/favorites with pagination and a default sort', async () => {
    const fetchMock = stubFetch();

    await listFavoriteRecipes({ page: 0, size: 10 });

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/recipes/favorites?');
    expect(url).toContain('page=0');
    expect(url).toContain('size=10');
    expect(url).toContain('sort=id%2Casc');
  });
});

describe('getRecipe', () => {
  it('requests the recipe by id', async () => {
    const fetchMock = stubFetch();

    await getRecipe(7);

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/recipes/7');
    expect(url).not.toContain('/stats');
  });
});

describe('getRecipeStats', () => {
  it('requests the recipe stats by id', async () => {
    const fetchMock = stubFetch();

    await getRecipeStats(7);

    const url = fetchMock.mock.calls[0][0] as string;
    expect(url).toContain('/api/recipes/7/stats');
  });
});

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
