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
  },
} as const;
