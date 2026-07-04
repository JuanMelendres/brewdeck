import type { ListCoffeesParams } from '@/lib/api/coffees';

export const keys = {
  dashboard: {
    summary: ['dashboard', 'summary'],
  },
  coffees: {
    list: (params: ListCoffeesParams) => ['coffees', 'list', params] as const,
  },
} as const;
