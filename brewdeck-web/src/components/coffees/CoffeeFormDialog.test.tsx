import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeFormDialog } from './CoffeeFormDialog';
import * as mutations from '@/hooks/useCoffeeMutations';
import { ApiError } from '@/lib/api/client';

const createMutate = vi.fn();
const updateMutate = vi.fn();

function mockHooks() {
  vi.spyOn(mutations, 'useCreateCoffee').mockReturnValue({ mutate: createMutate, isPending: false } as never);
  vi.spyOn(mutations, 'useUpdateCoffee').mockReturnValue({ mutate: updateMutate, isPending: false } as never);
}

beforeEach(() => {
  createMutate.mockReset();
  updateMutate.mockReset();
});

afterEach(() => vi.restoreAllMocks());

describe('CoffeeFormDialog', () => {
  it('blocks submit and shows a required error when name is empty', async () => {
    mockHooks();
    renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(createMutate).not.toHaveBeenCalled();
  });

  it('calls the create mutation with the entered values on valid submit', async () => {
    mockHooks();
    renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Mezcla' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual(expect.objectContaining({ name: 'Mezcla' }));
  });

  it('maps a server 400 validation error onto the field', async () => {
    mockHooks();
    createMutate.mockImplementation((_body, opts) => {
      opts.onError(new ApiError(400, 'Validation failed', '/api/coffees', { name: 'Coffee name is required' }));
    });
    renderWithTheme(<CoffeeFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText('Coffee name is required')).toBeInTheDocument();
  });
});
