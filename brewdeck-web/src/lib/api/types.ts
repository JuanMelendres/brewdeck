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
