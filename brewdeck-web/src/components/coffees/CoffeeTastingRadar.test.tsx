import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeTastingRadar } from './CoffeeTastingRadar';

describe('CoffeeTastingRadar', () => {
  it('renders axis labels when all four scores are present', () => {
    renderWithTheme(
      <CoffeeTastingRadar acidity={4} body={3} sweetness={5} bitterness={2} />,
    );
    expect(screen.getByText('Acidity')).toBeInTheDocument();
    expect(screen.getByText('Body')).toBeInTheDocument();
    expect(screen.getByText('Sweetness')).toBeInTheDocument();
    expect(screen.getByText('Bitterness')).toBeInTheDocument();
  });

  it('shows an empty state when any score is missing', () => {
    renderWithTheme(
      <CoffeeTastingRadar acidity={4} body={null} sweetness={5} bitterness={2} />,
    );
    expect(screen.getByText(/add tasting scores/i)).toBeInTheDocument();
    expect(screen.queryByText('Acidity')).not.toBeInTheDocument();
  });
});
