import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { StatCard } from './StatCard';

describe('StatCard', () => {
  it('renders the label and value', () => {
    renderWithTheme(<StatCard label="Coffees" value={5} />);
    expect(screen.getByText('Coffees')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('renders a string value such as an em dash', () => {
    renderWithTheme(<StatCard label="Average Rating" value="—" />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
