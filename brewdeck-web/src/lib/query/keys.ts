import type { ListBrewSessionsParams } from '@/lib/api/brewSessions';
import type { ListCoffeesParams } from '@/lib/api/coffees';
import type { ListRecipesParams } from '@/lib/api/recipes';

export const keys = {
  dashboard: {
    summary: ['dashboard', 'summary'],
  },
  coffees: {
    list: (params: ListCoffeesParams) => ['coffees', 'list', params] as const,
  },
  recipes: {
    list: (params: ListRecipesParams) => ['recipes', 'list', params] as const,
    detail: (id: number) => ['recipes', 'detail', id] as const,
    stats: (id: number) => ['recipes', 'stats', id] as const,
  },
  brewSessions: {
    list: (params: ListBrewSessionsParams) => ['brew-sessions', 'list', params] as const,
  },
} as const;
