import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeCards } from './RecipeCards';
import type { Recipe } from '@/lib/api/types';

const toggleMutate = vi.fn();
vi.mock('@/hooks/useRecipeMutations', () => ({
  useToggleFavorite: () => ({ mutate: toggleMutate, isPending: false }),
}));

const base: Recipe = {
  id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
  name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
  grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null, shareToken: null,
};

beforeEach(() => toggleMutate.mockReset());

describe('RecipeCards', () => {
  it('renders a card with the coffee, method, and the parameters that are set', () => {
    renderWithTheme(<RecipeCards recipes={[base]} />);

    expect(screen.getByRole('link', { name: 'Mezcla AeroPress' })).toHaveAttribute('href', '/recipes/1');
    expect(screen.getByText('Mezcla · AeroPress')).toBeInTheDocument();
    expect(screen.getByText('15 g')).toBeInTheDocument();
    expect(screen.getByText('230 g')).toBeInTheDocument();
    expect(screen.getByText('1:15')).toBeInTheDocument();
    expect(screen.getByText('90°C')).toBeInTheDocument();
    expect(screen.queryByText('Grind')).not.toBeInTheDocument();
    expect(screen.queryByText('Time')).not.toBeInTheDocument();
  });

  it('shows whether each recipe is a favorite', () => {
    const plain: Recipe = { ...base, id: 2, name: 'Plain V60', favorite: false };
    renderWithTheme(<RecipeCards recipes={[base, plain]} />);

    expect(screen.getByRole('button', { name: 'Favorite Mezcla AeroPress' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Favorite Plain V60' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('toggles the favorite from the heart', () => {
    renderWithTheme(<RecipeCards recipes={[base]} />);

    fireEvent.click(screen.getByRole('button', { name: 'Favorite Mezcla AeroPress' }));

    expect(toggleMutate).toHaveBeenCalledWith({ id: 1, favorite: false });
  });

  it('calls onEdit and onDelete with the card recipe', () => {
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderWithTheme(<RecipeCards recipes={[base]} onEdit={onEdit} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole('button', { name: 'Edit Mezcla AeroPress' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete Mezcla AeroPress' }));

    expect(onEdit).toHaveBeenCalledWith(base);
    expect(onDelete).toHaveBeenCalledWith(base);
  });
});
