import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeFormDialog } from './RecipeFormDialog';
import * as mutations from '@/hooks/useRecipeMutations';
import * as options from '@/hooks/useResourceOptions';
import { ApiError } from '@/lib/api/client';

const createMutate = vi.fn();
const updateMutate = vi.fn();

function mockAll() {
  vi.spyOn(mutations, 'useCreateRecipe').mockReturnValue({ mutate: createMutate, isPending: false } as never);
  vi.spyOn(mutations, 'useUpdateRecipe').mockReturnValue({ mutate: updateMutate, isPending: false } as never);
  vi.spyOn(options, 'useCoffeeOptions').mockReturnValue({ data: [{ id: 1, name: 'Mezcla' }], isLoading: false } as never);
  vi.spyOn(options, 'useMethodOptions').mockReturnValue({ data: [{ id: 2, name: 'AeroPress' }], isLoading: false } as never);
}

beforeEach(() => {
  createMutate.mockReset();
  updateMutate.mockReset();
});
afterEach(() => vi.restoreAllMocks());

describe('RecipeFormDialog', () => {
  it('renders the coffee and method options', () => {
    mockAll();
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    expect(screen.getByRole('option', { name: 'Mezcla' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'AeroPress' })).toBeInTheDocument();
  });

  it('blocks submit and shows required errors when empty', async () => {
    mockAll();
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(createMutate).not.toHaveBeenCalled();
  });

  it('calls create with coerced values on valid submit', async () => {
    mockAll();
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    fireEvent.change(screen.getByRole('combobox', { name: /coffee/i }), { target: { value: '1' } });
    fireEvent.change(screen.getByRole('combobox', { name: /brew method/i }), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Recipe' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual(
      expect.objectContaining({ coffeeId: 1, methodId: 2, name: 'My Recipe' }),
    );
  });

  it('maps a server 400 validation error onto the field', async () => {
    mockAll();
    createMutate.mockImplementation((_body, opts) =>
      opts.onError(new ApiError(400, 'Validation failed', '/api/recipes', { name: 'Recipe name is required' })),
    );
    renderWithTheme(<RecipeFormDialog open onClose={vi.fn()} />);
    fireEvent.change(screen.getByRole('combobox', { name: /coffee/i }), { target: { value: '1' } });
    fireEvent.change(screen.getByRole('combobox', { name: /brew method/i }), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(await screen.findByText('Recipe name is required')).toBeInTheDocument();
  });
});
