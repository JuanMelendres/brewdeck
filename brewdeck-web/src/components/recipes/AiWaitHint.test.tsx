import { act, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { AiWaitHint, LONG_WAIT_MS } from './AiWaitHint';

describe('AiWaitHint', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows nothing while the AI is idle', () => {
    renderWithTheme(<AiWaitHint active={false} />);

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('sets expectations while the AI works, then reassures on a long wait', () => {
    renderWithTheme(<AiWaitHint active />);
    expect(screen.getByRole('status')).toHaveTextContent('usually takes about 20 seconds');

    act(() => {
      vi.advanceTimersByTime(LONG_WAIT_MS);
    });

    expect(screen.getByRole('status')).toHaveTextContent('Still working');
  });

  it('starts over for the next request', () => {
    const { rerender } = renderWithTheme(<AiWaitHint active />);
    act(() => {
      vi.advanceTimersByTime(LONG_WAIT_MS);
    });
    rerender(<AiWaitHint active={false} />);
    rerender(<AiWaitHint active />);

    expect(screen.getByRole('status')).toHaveTextContent('usually takes about 20 seconds');
  });

  it('speaks Spanish for a Spanish user', () => {
    renderWithTheme(<AiWaitHint active />, { locale: 'es' });

    expect(screen.getByRole('status')).toHaveTextContent('suele tardar unos 20 segundos');
  });
});
