import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsTable } from './BrewSessionsTable';
import type { BrewSession } from '@/lib/api/types';
import { formatDateTime } from '@/lib/format/dates';

const base: BrewSession = {
  id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00Z',
  actualGrind: 'S3 5.5', actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
  rating: 9, adjustmentNotes: null,
};

const empty: BrewSession = {
  ...base, id: 2, recipeName: 'Plain V60', brewedAt: '2026-02-02T08:15:00Z',
  actualTemp: null, actualTime: null, tasteResult: null, rating: null,
};

describe('BrewSessionsTable', () => {
  it('renders session rows with the recipe link, formatted date, rating, and units', () => {
    renderWithTheme(<BrewSessionsTable sessions={[base, empty]} />);

    expect(screen.getByRole('link', { name: 'Mezcla AeroPress' })).toHaveAttribute('href', '/recipes/1');
    // Rendered in the viewer's zone, never the raw UTC string.
    expect(screen.getByText(formatDateTime(base.brewedAt))).toBeInTheDocument();
    expect(screen.queryByText(/2026-01-01T10:30/)).not.toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('/10')).toBeInTheDocument();
    expect(screen.getByText('90°C')).toBeInTheDocument();
    expect(screen.getByText('2:30')).toBeInTheDocument();
    expect(screen.getByText('Clean')).toBeInTheDocument();
  });

  it('shows a dash for each missing value', () => {
    renderWithTheme(<BrewSessionsTable sessions={[empty]} />);

    expect(screen.getByText(formatDateTime(empty.brewedAt))).toBeInTheDocument();
    // Null rating, actualTemp, actualTime, and tasteResult.
    expect(screen.getAllByText('—')).toHaveLength(4);
  });
});
