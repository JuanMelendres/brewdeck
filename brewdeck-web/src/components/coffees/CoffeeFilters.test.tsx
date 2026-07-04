import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeFilters } from './CoffeeFilters';

describe('CoffeeFilters', () => {
  it('calls onChange with the merged filters when a field changes', () => {
    const onChange = vi.fn();
    renderWithTheme(<CoffeeFilters value={{ origin: 'Veracruz' }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Blend' } });

    expect(onChange).toHaveBeenCalledWith({ origin: 'Veracruz', name: 'Blend' });
  });
});
