import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeesTable } from './CoffeesTable';
import type { Coffee } from '@/lib/api/types';

const coffee: Coffee = {
  id: 1, name: 'Mezcla Veracruz', brand: 'Local', origin: 'Veracruz', region: null, farm: null,
  producer: null, variety: null, process: 'Lavado', roastLevel: null, notesPrimary: null,
  notesSecondary: null, acidity: null, body: null, sweetness: null, bitterness: null,
  description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

describe('CoffeesTable', () => {
  it('renders a row with the coffee fields and an em dash for null roast', () => {
    renderWithTheme(<CoffeesTable coffees={[coffee]} />);
    expect(screen.getByRole('link', { name: 'Mezcla Veracruz' })).toHaveAttribute(
      'href',
      '/coffees/1',
    );
    expect(screen.getByText('Local')).toBeInTheDocument();
    expect(screen.getByText('Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Lavado')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('calls onEdit and onDelete with the row coffee when the action buttons are clicked', () => {
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderWithTheme(<CoffeesTable coffees={[coffee]} onEdit={onEdit} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole('button', { name: 'edit' }));
    fireEvent.click(screen.getByRole('button', { name: 'delete' }));

    expect(onEdit).toHaveBeenCalledWith(coffee);
    expect(onDelete).toHaveBeenCalledWith(coffee);
  });
});
