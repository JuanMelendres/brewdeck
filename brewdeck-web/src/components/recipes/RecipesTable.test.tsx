import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipesTable } from './RecipesTable';
import type { Recipe } from '@/lib/api/types';

const base: Recipe = {
  id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
  name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
  grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null, shareToken: null,
};

const other: Recipe = {
  ...base, id: 2, name: 'Plain V60', coffeeName: 'Other', methodName: 'V60',
  ratio: null, waterTemp: null, favorite: false,
};

describe('RecipesTable', () => {
  it('renders recipe rows with coffee/method names, a star for favorites and dashes for null/false', () => {
    renderWithTheme(<RecipesTable recipes={[base, other]} />);

    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mezcla AeroPress' })).toHaveAttribute(
      'href',
      '/recipes/1',
    );
    expect(screen.getByText('Mezcla')).toBeInTheDocument();
    expect(screen.getByText('AeroPress')).toBeInTheDocument();
    expect(screen.getByText('1:15')).toBeInTheDocument();
    expect(screen.getByText('90')).toBeInTheDocument();
    expect(screen.getByText('★')).toBeInTheDocument();

    // 'other' has null ratio, null waterTemp, and favorite=false → three dashes
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(3);
  });

  it('calls onEdit and onDelete with the row recipe when the action buttons are clicked', () => {
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderWithTheme(<RecipesTable recipes={[base]} onEdit={onEdit} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole('button', { name: 'edit' }));
    fireEvent.click(screen.getByRole('button', { name: 'delete' }));

    expect(onEdit).toHaveBeenCalledWith(base);
    expect(onDelete).toHaveBeenCalledWith(base);
  });
});
