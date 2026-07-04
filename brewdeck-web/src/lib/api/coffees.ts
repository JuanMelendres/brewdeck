import { apiFetch } from './client';
import type { Coffee, CoffeeFilters, PageResponse } from './types';

export type ListCoffeesParams = {
  page: number;
  size: number;
  sort?: string;
  filters?: CoffeeFilters;
};

export function listCoffees(params: ListCoffeesParams): Promise<PageResponse<Coffee>> {
  const query = new URLSearchParams();
  query.set('page', String(params.page));
  query.set('size', String(params.size));
  query.set('sort', params.sort ?? 'id,asc');

  const filters = params.filters ?? {};
  (Object.keys(filters) as Array<keyof CoffeeFilters>).forEach((key) => {
    const value = filters[key]?.trim();
    if (value) {
      query.set(key, value);
    }
  });

  return apiFetch<PageResponse<Coffee>>(`/api/coffees?${query.toString()}`);
}
