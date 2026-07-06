import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewMethodsTable } from './BrewMethodsTable';
import type { BrewMethod } from '@/lib/api/brewMethods';

const aeropress: BrewMethod = {
  id: 1,
  name: 'AeroPress',
  description: 'Immersion and pressure',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: null,
};

const v60: BrewMethod = { ...aeropress, id: 2, name: 'V60', description: null };

describe('BrewMethodsTable', () => {
  it('renders method rows and an em dash for a null description', () => {
    renderWithTheme(<BrewMethodsTable methods={[aeropress, v60]} />);

    expect(screen.getByText('AeroPress')).toBeInTheDocument();
    expect(screen.getByText('Immersion and pressure')).toBeInTheDocument();
    expect(screen.getByText('V60')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
