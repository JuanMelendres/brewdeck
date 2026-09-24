import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DeleteBrewMethodDialog } from './DeleteBrewMethodDialog';
import * as mutations from '@/hooks/useBrewMethodMutations';
import { ApiError } from '@/lib/api/client';
import type { BrewMethod } from '@/lib/api/brewMethods';

const deleteMutate = vi.fn();

const myMethod: BrewMethod = {
  id: 3,
  name: 'My Moka',
  description: null,
  shared: false,
  createdAt: '2026-01-01T00:00:00',
};

afterEach(() => {
  deleteMutate.mockReset();
  vi.restoreAllMocks();
});

describe('DeleteBrewMethodDialog', () => {
  it('calls the delete mutation with the method id when confirmed', () => {
    vi.spyOn(mutations, 'useDeleteBrewMethod').mockReturnValue({ mutate: deleteMutate, isPending: false } as never);
    renderWithTheme(<DeleteBrewMethodDialog open method={myMethod} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(deleteMutate.mock.calls[0][0]).toBe(3);
  });

  it('explains a 409 as the method still being used by recipes', async () => {
    deleteMutate.mockImplementation((_id, opts) => {
      opts.onError(new ApiError(409, 'Data integrity violation', '/api/brew-methods/3'));
    });
    vi.spyOn(mutations, 'useDeleteBrewMethod').mockReturnValue({ mutate: deleteMutate, isPending: false } as never);
    renderWithTheme(<DeleteBrewMethodDialog open method={myMethod} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(await screen.findByText(/used by one or more recipes/i)).toBeInTheDocument();
  });
});
