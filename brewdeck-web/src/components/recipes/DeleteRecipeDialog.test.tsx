import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import * as mutations from '@/hooks/useRecipeMutations';
import type { Recipe } from '@/lib/api/types';

const deleteMutate = vi.fn();

const recipe: Recipe = {
  id: 7, coffeeId: 1, coffeeName: 'Mezcla', methodId: 2, methodName: 'AeroPress',
  name: 'My Recipe', coffeeGrams: null, waterGrams: null, ratio: null, grindSetting: null,
  waterTemp: null, brewTime: null, steps: null, expectedTaste: null, favorite: false,
  createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('DeleteRecipeDialog', () => {
  it('calls the delete mutation with the recipe id when confirmed', () => {
    vi.spyOn(mutations, 'useDeleteRecipe').mockReturnValue({ mutate: deleteMutate, isPending: false } as never);
    renderWithTheme(<DeleteRecipeDialog open recipe={recipe} onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));
    expect(deleteMutate.mock.calls[0][0]).toBe(7);
  });
});
