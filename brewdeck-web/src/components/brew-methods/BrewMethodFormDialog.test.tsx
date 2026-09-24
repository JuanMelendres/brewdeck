import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewMethodFormDialog } from './BrewMethodFormDialog';
import * as mutations from '@/hooks/useBrewMethodMutations';
import { ApiError } from '@/lib/api/client';
import type { BrewMethod } from '@/lib/api/brewMethods';

const createMutate = vi.fn();
const updateMutate = vi.fn();

const myMethod: BrewMethod = {
  id: 3,
  name: 'My Moka',
  description: 'Stovetop',
  shared: false,
  createdAt: '2026-01-01T00:00:00',
};

function mockHooks() {
  vi.spyOn(mutations, 'useCreateBrewMethod').mockReturnValue({ mutate: createMutate, isPending: false } as never);
  vi.spyOn(mutations, 'useUpdateBrewMethod').mockReturnValue({ mutate: updateMutate, isPending: false } as never);
}

beforeEach(() => {
  createMutate.mockReset();
  updateMutate.mockReset();
});

afterEach(() => vi.restoreAllMocks());

describe('BrewMethodFormDialog', () => {
  it('blocks submit and shows a required error when name is empty', async () => {
    mockHooks();
    renderWithTheme(<BrewMethodFormDialog open onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(createMutate).not.toHaveBeenCalled();
  });

  it('tells the user a new method is private', () => {
    mockHooks();
    renderWithTheme(<BrewMethodFormDialog open onClose={vi.fn()} />);

    expect(screen.getByText(/only you can see and use them/i)).toBeInTheDocument();
  });

  it('calls the create mutation with the entered values on valid submit', async () => {
    mockHooks();
    renderWithTheme(<BrewMethodFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Clever Dripper' } });
    fireEvent.change(screen.getByLabelText(/^Description/), { target: { value: 'Steep and release' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual({
      name: 'Clever Dripper',
      description: 'Steep and release',
    });
  });

  it('prefills the form and calls update when editing', async () => {
    mockHooks();
    renderWithTheme(<BrewMethodFormDialog open method={myMethod} onClose={vi.fn()} />);

    expect(screen.getByLabelText(/^Name/)).toHaveValue('My Moka');
    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Bialetti' } });
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => expect(updateMutate).toHaveBeenCalledTimes(1));
    expect(updateMutate.mock.calls[0][0]).toEqual({
      id: 3,
      body: { name: 'My Bialetti', description: 'Stovetop' },
    });
  });

  it('maps a server 400 validation error onto the field', async () => {
    mockHooks();
    createMutate.mockImplementation((_body, opts) => {
      opts.onError(
        new ApiError(400, 'Validation failed', '/api/brew-methods', { name: 'Method name is required' }),
      );
    });
    renderWithTheme(<BrewMethodFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText('Method name is required')).toBeInTheDocument();
  });

  it('shows a duplicate-name error on a 409', async () => {
    mockHooks();
    createMutate.mockImplementation((_body, opts) => {
      opts.onError(new ApiError(409, 'Data integrity violation', '/api/brew-methods'));
    });
    renderWithTheme(<BrewMethodFormDialog open onClose={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Moka' } });
    fireEvent.click(screen.getByRole('button', { name: /create/i }));

    expect(await screen.findByText(/already have a brew method with this name/i)).toBeInTheDocument();
  });
});
