import type { ListBrewMethodsParams } from '@/lib/api/brewMethods';
import type { ListBrewSessionsParams } from '@/lib/api/brewSessions';
import type { ListCoffeesParams } from '@/lib/api/coffees';
import type { ListFavoriteRecipesParams, ListRecipesParams } from '@/lib/api/recipes';

export const keys = {
  featureFlags: {
    // Scoped by user id so a different signed-in user re-fetches (rollout can differ per user).
    forUser: (userId: number | null) => ['feature-flags', userId] as const,
  },
  dashboard: {
    summary: ['dashboard', 'summary'],
  },
  coffees: {
    list: (params: ListCoffeesParams) => ['coffees', 'list', params] as const,
    mostUsed: (limit: number) => ['coffees', 'most-used', limit] as const,
    detail: (id: number) => ['coffees', 'detail', id] as const,
  },
  recipes: {
    list: (params: ListRecipesParams) => ['recipes', 'list', params] as const,
    favorites: (params: ListFavoriteRecipesParams) => ['recipes', 'favorites', params] as const,
    topRated: (limit: number) => ['recipes', 'top-rated', limit] as const,
    mostBrewed: (limit: number) => ['recipes', 'most-brewed', limit] as const,
    detail: (id: number) => ['recipes', 'detail', id] as const,
    stats: (id: number) => ['recipes', 'stats', id] as const,
    public: (token: string) => ['recipes', 'public', token] as const,
  },
  brewMethods: {
    list: (params: ListBrewMethodsParams) => ['brew-methods', 'list', params] as const,
    usage: ['brew-methods', 'usage'],
  },
  brewSessions: {
    list: (params: ListBrewSessionsParams) => ['brew-sessions', 'list', params] as const,
    byRecipe: (recipeId: number) => ['brew-sessions', 'by-recipe', recipeId] as const,
  },
} as const;
