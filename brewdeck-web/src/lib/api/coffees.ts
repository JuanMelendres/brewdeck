import { apiFetch } from './client';
import type { Coffee, CoffeeFilters, PageResponse } from './types';
import type { CoffeeFormValues } from '@/lib/validation/coffeeSchema';

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

export type MostUsedCoffee = {
  coffeeId: number;
  coffeeName: string;
  recipeCount: number;
};

export function listMostUsedCoffees(limit = 5): Promise<MostUsedCoffee[]> {
  const query = new URLSearchParams();
  query.set('limit', String(limit));

  return apiFetch<MostUsedCoffee[]>(`/api/coffees/most-used?${query.toString()}`);
}

export function createCoffee(body: CoffeeFormValues): Promise<Coffee> {
  return apiFetch<Coffee>('/api/coffees', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateCoffee(id: number, body: CoffeeFormValues): Promise<Coffee> {
  return apiFetch<Coffee>(`/api/coffees/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function deleteCoffee(id: number): Promise<void> {
  return apiFetch<void>(`/api/coffees/${id}`, { method: 'DELETE' });
}
