import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ShareRecipeDialog } from './ShareRecipeDialog';
import type { Recipe } from '@/lib/api/types';
import * as recipesApi from '@/lib/api/recipes';

function baseRecipe(overrides: Partial<Recipe> = {}): Recipe {
  return {
    id: 1,
    coffeeId: 1,
    coffeeName: 'Ethiopia',
    methodId: 1,
    methodName: 'V60',
    name: 'Morning Cup',
    coffeeGrams: 15,
    waterGrams: 250,
    ratio: '1:16',
    grindSetting: 'Medium',
    waterTemp: 94,
    brewTime: '3:00',
    steps: 'Bloom then pour',
    expectedTaste: 'Floral',
    favorite: false,
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: null,
    shareToken: null,
    ...overrides,
  };
}

function renderDialog(recipe: Recipe) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <ShareRecipeDialog open recipe={recipe} onClose={() => {}} />
    </QueryClientProvider>,
  );
}

describe('ShareRecipeDialog', () => {
  beforeEach(() => {
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
  });
  afterEach(() => vi.restoreAllMocks());

  it('renders the not-shared state with a Create link button', () => {
    renderDialog(baseRecipe({ shareToken: null }));
    expect(screen.getByRole('button', { name: /create link/i })).toBeInTheDocument();
  });

  it('calls shareRecipe when Create link is clicked', async () => {
    const spy = vi
      .spyOn(recipesApi, 'shareRecipe')
      .mockResolvedValue(baseRecipe({ shareToken: 'tok-1' }));
    renderDialog(baseRecipe({ shareToken: null }));

    await userEvent.click(screen.getByRole('button', { name: /create link/i }));

    await waitFor(() => expect(spy).toHaveBeenCalledWith(1));
  });

  it('renders the shared state with the link, Copy, and Stop sharing', () => {
    renderDialog(baseRecipe({ shareToken: 'tok-1' }));
    expect(screen.getByRole('button', { name: /copy/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /stop sharing/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue(/\/share\/tok-1$/)).toBeInTheDocument();
  });

  it('calls unshareRecipe when Stop sharing is clicked', async () => {
    const spy = vi
      .spyOn(recipesApi, 'unshareRecipe')
      .mockResolvedValue(baseRecipe({ shareToken: null }));
    renderDialog(baseRecipe({ shareToken: 'tok-1' }));

    await userEvent.click(screen.getByRole('button', { name: /stop sharing/i }));

    await waitFor(() => expect(spy).toHaveBeenCalledWith(1));
  });

  it('copies the link to the clipboard when Copy is clicked', async () => {
    renderDialog(baseRecipe({ shareToken: 'tok-1' }));

    await userEvent.click(screen.getByRole('button', { name: /copy/i }));

    await waitFor(() =>
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        expect.stringMatching(/\/share\/tok-1$/),
      ),
    );
  });
});
