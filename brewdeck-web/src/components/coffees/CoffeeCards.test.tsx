import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeCards } from './CoffeeCards';
import type { Coffee } from '@/lib/api/types';

const coffee: Coffee = {
  id: 1, name: 'Mezcla Veracruz', brand: 'Local', origin: 'Veracruz', region: 'Coatepec', farm: null,
  producer: null, variety: null, process: 'Lavado', roastLevel: 'Medio', notesPrimary: 'Cardamomo',
  notesSecondary: null, acidityScore: 4, bodyScore: 3, sweetnessScore: null, bitternessScore: null,
  description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

describe('CoffeeCards', () => {
  it('renders a card with the coffee details', () => {
    renderWithTheme(<CoffeeCards coffees={[coffee]} />);

    const card = screen.getByRole('article');
    expect(screen.getByRole('link', { name: 'Mezcla Veracruz' })).toHaveAttribute('href', '/coffees/1');
    expect(card).toHaveTextContent('Local');
    expect(card).toHaveTextContent('Veracruz · Coatepec');
    expect(screen.getByText('Medio')).toBeInTheDocument();
    expect(screen.getByText('Lavado')).toBeInTheDocument();
    expect(screen.getByText('Cardamomo')).toBeInTheDocument();
  });

  it('shows only the tasting scores that are set', () => {
    renderWithTheme(<CoffeeCards coffees={[coffee]} />);

    expect(screen.getByText('Acidity')).toBeInTheDocument();
    expect(screen.getByText('4/5')).toBeInTheDocument();
    expect(screen.getByText('Body')).toBeInTheDocument();
    expect(screen.queryByText('Sweetness')).not.toBeInTheDocument();
  });

  it('leaves out empty fields instead of showing placeholders', () => {
    const bare: Coffee = { ...coffee, brand: null, origin: null, region: null, process: null, roastLevel: null, notesPrimary: null, acidityScore: null, bodyScore: null };
    renderWithTheme(<CoffeeCards coffees={[bare]} />);

    expect(screen.getByRole('article')).toHaveTextContent(/^Mezcla Veracruz$/);
  });

  it('calls onEdit and onDelete with the card coffee', () => {
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    renderWithTheme(<CoffeeCards coffees={[coffee]} onEdit={onEdit} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole('button', { name: 'Edit Mezcla Veracruz' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete Mezcla Veracruz' }));

    expect(onEdit).toHaveBeenCalledWith(coffee);
    expect(onDelete).toHaveBeenCalledWith(coffee);
  });
});
