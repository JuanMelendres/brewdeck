import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewMethodsTable } from './BrewMethodsTable';
import type { BrewMethod } from '@/lib/api/brewMethods';

const aeropress: BrewMethod = {
  id: 1,
  name: 'AeroPress',
  description: 'Immersion and pressure',
  shared: true,
  createdAt: '2026-01-01T00:00:00',
};

const v60: BrewMethod = { ...aeropress, id: 2, name: 'V60', description: null };

const myMethod: BrewMethod = {
  ...aeropress,
  id: 3,
  name: 'My Moka',
  description: 'Stovetop',
  shared: false,
};

describe('BrewMethodsTable', () => {
  it('renders method rows and an em dash for a null description', () => {
    renderWithTheme(<BrewMethodsTable methods={[aeropress, v60]} />);

    expect(screen.getByText('AeroPress')).toBeInTheDocument();
    expect(screen.getByText('Immersion and pressure')).toBeInTheDocument();
    expect(screen.getByText('V60')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('labels shared and private methods', () => {
    renderWithTheme(<BrewMethodsTable methods={[aeropress, myMethod]} />);

    expect(screen.getByText('Shared')).toBeInTheDocument();
    expect(screen.getByText('Mine')).toBeInTheDocument();
  });

  it('offers edit and delete only for the user’s own methods', () => {
    renderWithTheme(<BrewMethodsTable methods={[aeropress, myMethod]} />);

    expect(screen.queryByRole('button', { name: /edit aeropress/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete aeropress/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /edit my moka/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /delete my moka/i })).toBeInTheDocument();
  });

  it('reports the clicked method to the edit and delete handlers', () => {
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderWithTheme(<BrewMethodsTable methods={[myMethod]} onEdit={onEdit} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole('button', { name: /edit my moka/i }));
    fireEvent.click(screen.getByRole('button', { name: /delete my moka/i }));

    expect(onEdit).toHaveBeenCalledWith(myMethod);
    expect(onDelete).toHaveBeenCalledWith(myMethod);
  });
});
