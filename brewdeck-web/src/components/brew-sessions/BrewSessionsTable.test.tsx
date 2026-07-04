import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsTable } from './BrewSessionsTable';
import type { BrewSession } from '@/lib/api/types';

const base: BrewSession = {
  id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00',
  actualGrind: 'S3 5.5', actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
  rating: 9, adjustmentNotes: null,
};

const empty: BrewSession = {
  ...base, id: 2, recipeName: 'Plain V60', brewedAt: '2026-02-02T08:15:00',
  actualTemp: null, actualTime: null, tasteResult: null, rating: null,
};

describe('BrewSessionsTable', () => {
  it('renders session rows with recipe name, formatted brewedAt, and dashes for nulls', () => {
    renderWithTheme(<BrewSessionsTable sessions={[base, empty]} />);

    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
    expect(screen.getByText('2026-01-01 10:30')).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('90')).toBeInTheDocument();
    expect(screen.getByText('2:30')).toBeInTheDocument();
    expect(screen.getByText('Clean')).toBeInTheDocument();

    expect(screen.getByText('2026-02-02 08:15')).toBeInTheDocument();
    // 'empty' row has null rating, actualTemp, actualTime, tasteResult → four dashes
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(4);
  });
});
