import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DeleteCoffeeDialog } from './DeleteCoffeeDialog';
import * as mutations from '@/hooks/useCoffeeMutations';
import type { Coffee } from '@/lib/api/types';

const deleteMutate = vi.fn();

const coffee: Coffee = {
  id: 7, name: 'Mezcla', brand: null, origin: null, region: null, farm: null, producer: null,
  variety: null, process: null, roastLevel: null, notesPrimary: null, notesSecondary: null,
  acidityScore: null, bodyScore: null, sweetnessScore: null, bitternessScore: null, description: null,
  createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('DeleteCoffeeDialog', () => {
  it('calls the delete mutation with the coffee id when confirmed', () => {
    vi.spyOn(mutations, 'useDeleteCoffee').mockReturnValue({ mutate: deleteMutate, isPending: false } as never);
    renderWithTheme(<DeleteCoffeeDialog open coffee={coffee} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(deleteMutate.mock.calls[0][0]).toBe(7);
  });
});
