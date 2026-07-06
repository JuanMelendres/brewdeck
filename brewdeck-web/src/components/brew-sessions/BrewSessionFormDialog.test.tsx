import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionFormDialog } from './BrewSessionFormDialog';
import * as mutations from '@/hooks/useBrewSessionMutations';
import * as options from '@/hooks/useResourceOptions';
import { ApiError } from '@/lib/api/client';

const createMutate = vi.fn();

function mockAll() {
  vi.spyOn(mutations, 'useCreateBrewSession').mockReturnValue({
    mutate: createMutate,
    isPending: false,
  } as never);
  vi.spyOn(options, 'useRecipeOptions').mockReturnValue({
    data: [{ id: 1, name: 'Mezcla AeroPress' }],
    isLoading: false,
  } as never);
}

beforeEach(() => createMutate.mockReset());
afterEach(() => vi.restoreAllMocks());

describe('BrewSessionFormDialog', () => {
  it('renders the recipe options', () => {
    mockAll();
    renderWithTheme(<BrewSessionFormDialog open onClose={vi.fn()} />);
    expect(screen.getByRole('option', { name: 'Mezcla AeroPress' })).toBeInTheDocument();
  });

  it('blocks submit and shows the required error when no recipe is chosen', async () => {
    mockAll();
    renderWithTheme(<BrewSessionFormDialog open onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(await screen.findByText('Recipe is required')).toBeInTheDocument();
    expect(createMutate).not.toHaveBeenCalled();
  });

  it('calls create with coerced values on valid submit', async () => {
    mockAll();
    renderWithTheme(<BrewSessionFormDialog open onClose={vi.fn()} />);
    fireEvent.change(screen.getByRole('combobox', { name: /recipe/i }), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText(/^Rating/), { target: { value: '9' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual(
      expect.objectContaining({ recipeId: 1, rating: 9 }),
    );
  });

  it('prefills the recipe when a recipeId is provided', () => {
    mockAll();
    renderWithTheme(<BrewSessionFormDialog open recipeId={1} onClose={vi.fn()} />);
    expect(screen.getByRole('combobox', { name: /recipe/i })).toHaveValue('1');
  });

  it('maps a server 400 validation error onto the field', async () => {
    mockAll();
    createMutate.mockImplementation((_body, opts) =>
      opts?.onError?.(
        new ApiError(400, 'Validation failed', '/api/brew-sessions', {
          rating: 'Rating must not exceed 10',
        }),
      ),
    );
    renderWithTheme(<BrewSessionFormDialog open onClose={vi.fn()} />);
    fireEvent.change(screen.getByRole('combobox', { name: /recipe/i }), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(await screen.findByText('Rating must not exceed 10')).toBeInTheDocument();
  });
});
