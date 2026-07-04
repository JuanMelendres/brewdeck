import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeFilters } from './RecipeFilters';

describe('RecipeFilters', () => {
  it('calls onChange with the merged name when the name field changes', () => {
    const onChange = vi.fn();
    renderWithTheme(<RecipeFilters value={{ favorite: true }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'AeroPress' } });

    expect(onChange).toHaveBeenCalledWith({ favorite: true, name: 'AeroPress' });
  });

  it('calls onChange with favorite true when the Favorites only box is checked', () => {
    const onChange = vi.fn();
    renderWithTheme(<RecipeFilters value={{}} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText('Favorites only'));

    expect(onChange).toHaveBeenCalledWith({ favorite: true });
  });
});
