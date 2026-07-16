import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeDetailView } from './RecipeDetailView';
import * as recipeHooks from '@/hooks/useRecipe';
import * as historyHook from '@/hooks/useRecipeBrewSessions';
import type { BrewSession, PageResponse, Recipe, RecipeStats } from '@/lib/api/types';
import { ApiError } from '@/lib/api/client';

type RecipeReturn = ReturnType<typeof recipeHooks.useRecipe>;
type StatsReturn = ReturnType<typeof recipeHooks.useRecipeStats>;
type HistoryReturn = ReturnType<typeof historyHook.useRecipeBrewSessions>;

const { improveMutate } = vi.hoisted(() => ({ improveMutate: vi.fn() }));
const { downloadRecipePdfMock } = vi.hoisted(() => ({ downloadRecipePdfMock: vi.fn() }));

// AI assistant flag on: these tests cover the enabled-feature behaviour of the AI button.
vi.mock('@/lib/featureFlags/FeatureFlagProvider', () => ({
  useFeatureFlag: () => true,
  useFeatureFlags: () => ({ flags: { aiRecipeAssistant: true }, status: 'ready' }),
}));
vi.mock('@/hooks/useImproveRecipe', () => ({
  useImproveRecipe: () => ({ mutate: improveMutate, isPending: false }),
}));
vi.mock('@/hooks/useRecipeMutations', () => ({
  useCreateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/hooks/useResourceOptions', () => ({
  useCoffeeOptions: () => ({ data: [], isLoading: false }),
  useMethodOptions: () => ({ data: [], isLoading: false }),
}));
vi.mock('@/hooks/useSuggestRecipe', () => ({
  useSuggestRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/hooks/useShareRecipe', () => ({
  useShareRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useUnshareRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/lib/pdf/recipePdf', async (importActual) => ({
  ...(await importActual<typeof import('@/lib/pdf/recipePdf')>()),
  downloadRecipePdf: downloadRecipePdfMock,
}));

function sessionsPage(content: BrewSession[]): PageResponse<BrewSession> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1, first: true, last: true };
}

const recipe: Recipe = {
  id: 1,
  coffeeId: 1,
  coffeeName: 'Mezcla Veracruz',
  methodId: 1,
  methodName: 'AeroPress',
  name: 'Mezcla Veracruz AeroPress',
  coffeeGrams: 15,
  waterGrams: 230,
  ratio: '1:15',
  grindSetting: 'Timemore S3 - 5.5',
  waterTemp: 90,
  brewTime: '2:30',
  steps: 'Bloom 30s, stir gently, press slowly.',
  expectedTaste: 'Clean, aromatic, spicy, balanced.',
  favorite: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: null,
  shareToken: null,
};

const ratedSession: BrewSession = {
  id: 1,
  recipeId: 1,
  recipeName: 'Mezcla Veracruz AeroPress',
  brewedAt: '2026-04-21T10:00:00',
  actualGrind: 'Timemore S3 - 5.5',
  actualTemp: 91,
  actualTime: '2:20',
  tasteResult: 'Bright',
  rating: 9,
  adjustmentNotes: 'Grind finer next time.',
};

const unratedSession: BrewSession = { ...ratedSession, id: 2, rating: null };

function mockRecipe(value: Partial<RecipeReturn>) {
  vi.spyOn(recipeHooks, 'useRecipe').mockReturnValue(value as RecipeReturn);
}

function mockStats(value: Partial<StatsReturn>) {
  vi.spyOn(recipeHooks, 'useRecipeStats').mockReturnValue(value as StatsReturn);
}

function mockHistory(value: Partial<HistoryReturn>) {
  vi.spyOn(historyHook, 'useRecipeBrewSessions').mockReturnValue(value as HistoryReturn);
}

beforeEach(() => {
  improveMutate.mockReset();
  downloadRecipePdfMock.mockReset();
  mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });
});

afterEach(() => vi.restoreAllMocks());

describe('RecipeDetailView', () => {
  it('shows a spinner while the recipe is loading', () => {
    mockRecipe({ isLoading: true, isError: false, data: undefined });
    mockStats({ isLoading: true, isError: false, data: undefined });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state when the recipe fails to load', () => {
    mockRecipe({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    mockStats({ isLoading: false, isError: false, data: undefined });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByText(/could not load recipe\./i)).toBeInTheDocument();
  });

  it('renders recipe details and stat cards on success', () => {
    const stats: RecipeStats = {
      recipeId: 1,
      totalSessions: 3,
      averageRating: 8.5,
      lastBrewedAt: '2026-07-05T10:00:00',
    };
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: stats });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('heading', { name: 'Mezcla Veracruz AeroPress' })).toBeInTheDocument();
    expect(screen.getByText('Favorite')).toBeInTheDocument();
    expect(screen.getByText('Mezcla Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Total Sessions')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('8.5')).toBeInTheDocument();
  });

  it('renders an em dash for a null average rating', () => {
    const stats: RecipeStats = {
      recipeId: 1,
      totalSessions: 0,
      averageRating: null,
      lastBrewedAt: null,
    };
    mockRecipe({ isLoading: false, isError: false, data: { ...recipe, favorite: false } });
    mockStats({ isLoading: false, isError: false, data: stats });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByText('Average Rating')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(1);
    expect(screen.queryByText('Favorite')).not.toBeInTheDocument();
  });

  it('shows an inline stats error when statistics fail but the recipe loaded', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('heading', { name: 'Mezcla Veracruz AeroPress' })).toBeInTheDocument();
    expect(screen.getByText(/could not load recipe statistics\./i)).toBeInTheDocument();
  });

  it('renders the brew history table when sessions exist', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({
      isLoading: false,
      isError: false,
      data: { recipeId: 1, totalSessions: 1, averageRating: 9, lastBrewedAt: '2026-07-05T10:00:00' },
    });
    mockHistory({
      isLoading: false,
      isError: false,
      data: sessionsPage([
        {
          id: 5,
          recipeId: 1,
          recipeName: 'Mezcla Veracruz AeroPress',
          brewedAt: '2026-07-05T10:00:00',
          actualGrind: null,
          actualTemp: 90,
          actualTime: '2:30',
          tasteResult: 'Balanced',
          rating: 9,
          adjustmentNotes: null,
        },
      ]),
    });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByText('Brew history')).toBeInTheDocument();
    expect(screen.getByText('Balanced')).toBeInTheDocument();
  });

  it('shows an empty state when the recipe has no brew sessions', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({
      isLoading: false,
      isError: false,
      data: { recipeId: 1, totalSessions: 0, averageRating: null, lastBrewedAt: null },
    });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByText(/no brew sessions yet for this recipe/i)).toBeInTheDocument();
  });

  it('disables Improve with AI when there is no rated history', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([unratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('button', { name: /improve with ai/i })).toBeDisabled();
  });

  it('enables Improve with AI when at least one session is rated', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([ratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('button', { name: /improve with ai/i })).toBeEnabled();
  });

  it('opens the pre-filled edit dialog with the rationale on success', () => {
    improveMutate.mockImplementation((_id: number, { onSuccess }: { onSuccess: (data: unknown) => void }) =>
      onSuccess({
        coffeeGrams: 16,
        waterGrams: 240,
        ratio: '1:15',
        grindSetting: 'Timemore S3 - 5.0',
        waterTemp: 92,
        brewTime: '2:15',
        steps: 'Grind finer and shorten the brew.',
        rationale: 'Finer grind improves sweetness.',
      }),
    );
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([ratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /improve with ai/i }));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Edit recipe')).toBeInTheDocument();
    expect(screen.getByText('Finer grind improves sweetness.')).toBeInTheDocument();
  });

  it('shows a needs-history message when improve returns 422', () => {
    improveMutate.mockImplementation((_id: number, { onError }: { onError: (error: unknown) => void }) =>
      onError(new ApiError(422, 'Recipe has no rated brew sessions to improve from')),
    );
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([ratedSession]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /improve with ai/i }));

    expect(screen.getByText(/log a rated brew for this recipe first/i)).toBeInTheDocument();
  });

  it('renders the Export PDF button when the recipe is loaded', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('button', { name: /export pdf/i })).toBeInTheDocument();
  });

  it('downloads the PDF for the current recipe on click', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /export pdf/i }));

    expect(downloadRecipePdfMock).toHaveBeenCalledWith(recipe);
  });

  it('shows an error alert when PDF generation throws', () => {
    downloadRecipePdfMock.mockImplementation(() => {
      throw new Error('boom');
    });
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /export pdf/i }));

    expect(screen.getByText(/could not generate the pdf/i)).toBeInTheDocument();
  });

  it('opens the share dialog when Share is clicked', async () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: undefined });
    mockHistory({ isLoading: false, isError: false, data: sessionsPage([]) });

    renderWithTheme(<RecipeDetailView recipeId={1} />);
    await userEvent.click(await screen.findByRole('button', { name: /^share$/i }));

    expect(await screen.findByRole('dialog', { name: /share recipe/i })).toBeInTheDocument();
  });
});
