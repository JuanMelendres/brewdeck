'use client';

import { useQuery } from '@tanstack/react-query';
import { listCoffees } from '@/lib/api/coffees';
import { listBrewMethods } from '@/lib/api/brewMethods';
import { listRecipes } from '@/lib/api/recipes';

export type ResourceOption = { id: number; name: string };

export function useCoffeeOptions() {
  return useQuery({
    queryKey: ['coffees', 'options'],
    queryFn: () => listCoffees({ page: 0, size: 100 }),
    select: (page): ResourceOption[] => page.content.map((c) => ({ id: c.id, name: c.name })),
  });
}

export function useMethodOptions() {
  return useQuery({
    queryKey: ['brew-methods', 'options'],
    queryFn: () => listBrewMethods({ page: 0, size: 100 }),
    select: (page): ResourceOption[] => page.content.map((m) => ({ id: m.id, name: m.name })),
  });
}

export function useRecipeOptions() {
  return useQuery({
    queryKey: ['recipes', 'options'],
    queryFn: () => listRecipes({ page: 0, size: 100 }),
    select: (page): ResourceOption[] => page.content.map((r) => ({ id: r.id, name: r.name })),
  });
}
