export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ErrorResponse = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
};

export type DashboardSummary = {
  totalCoffees: number;
  totalBrewMethods: number;
  totalRecipes: number;
  favoriteRecipes: number;
  totalBrewSessions: number;
  averageSessionRating: number | null;
};

export type Coffee = {
  id: number;
  name: string;
  brand: string | null;
  origin: string | null;
  region: string | null;
  farm: string | null;
  producer: string | null;
  variety: string | null;
  process: string | null;
  roastLevel: string | null;
  notesPrimary: string | null;
  notesSecondary: string | null;
  acidity: string | null;
  body: string | null;
  sweetness: string | null;
  bitterness: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string | null;
};

export type CoffeeFilters = {
  name?: string;
  origin?: string;
  roastLevel?: string;
  process?: string;
};

export type Recipe = {
  id: number;
  coffeeId: number;
  coffeeName: string;
  methodId: number;
  methodName: string;
  name: string;
  coffeeGrams: number | null;
  waterGrams: number | null;
  ratio: string | null;
  grindSetting: string | null;
  waterTemp: number | null;
  brewTime: string | null;
  steps: string | null;
  expectedTaste: string | null;
  favorite: boolean;
  createdAt: string;
  updatedAt: string | null;
};

export type RecipeFilters = {
  name?: string;
  favorite?: boolean;
};

export type RecipeStats = {
  recipeId: number;
  totalSessions: number;
  averageRating: number | null;
  lastBrewedAt: string | null;
};

export type TopRatedRecipe = {
  recipeId: number;
  recipeName: string;
  averageRating: number | null;
  totalSessions: number;
};

export type BrewSession = {
  id: number;
  recipeId: number;
  recipeName: string;
  brewedAt: string;
  actualGrind: string | null;
  actualTemp: number | null;
  actualTime: string | null;
  tasteResult: string | null;
  rating: number | null;
  adjustmentNotes: string | null;
};

export type BrewSessionFilters = {
  rating?: number;
};
