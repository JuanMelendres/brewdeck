import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
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
    expect(screen.getByText('Mezcla Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Local')).toBeInTheDocument();
    expect(screen.getByText('Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Lavado')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
