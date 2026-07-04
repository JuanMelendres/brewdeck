import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionFilters } from './BrewSessionFilters';

describe('BrewSessionFilters', () => {
  it('calls onChange with a numeric rating when a value is entered', () => {
    const onChange = vi.fn();
    renderWithTheme(<BrewSessionFilters value={{}} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Rating'), { target: { value: '7' } });

    expect(onChange).toHaveBeenCalledWith({ rating: 7 });
  });

  it('calls onChange with rating undefined when the field is cleared', () => {
    const onChange = vi.fn();
    renderWithTheme(<BrewSessionFilters value={{ rating: 7 }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Rating'), { target: { value: '' } });

    expect(onChange).toHaveBeenCalledWith({ rating: undefined });
  });
});
