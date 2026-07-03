import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ErrorState } from './ErrorState';

describe('ErrorState', () => {
  it('shows the message', () => {
    renderWithTheme(<ErrorState message="Something failed" />);
    expect(screen.getByText('Something failed')).toBeInTheDocument();
  });

  it('calls onRetry when the retry button is clicked', async () => {
    const onRetry = vi.fn();
    renderWithTheme(<ErrorState message="Failed" onRetry={onRetry} />);

    await userEvent.click(screen.getByRole('button', { name: /retry/i }));

    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
