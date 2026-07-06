import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { MethodUsage } from './MethodUsage';
import * as hook from '@/hooks/useMethodUsage';
import type { MethodUsage as MethodUsageType } from '@/lib/api/brewMethods';

type HookReturn = ReturnType<typeof hook.useMethodUsage>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useMethodUsage').mockReturnValue(value as HookReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('MethodUsage', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<MethodUsage />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<MethodUsage />);
    expect(screen.getByText(/could not load method usage/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no methods', () => {
    mockHook({ isLoading: false, isError: false, data: [] });
    renderWithTheme(<MethodUsage />);
    expect(screen.getByText(/no brew methods yet/i)).toBeInTheDocument();
  });

  it('renders method rows with recipe counts', () => {
    const data: MethodUsageType[] = [
      { methodId: 1, methodName: 'AeroPress', recipeCount: 5 },
      { methodId: 2, methodName: 'V60', recipeCount: 0 },
    ];
    mockHook({ isLoading: false, isError: false, data });

    renderWithTheme(<MethodUsage />);

    expect(screen.getByText('AeroPress')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('V60')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });
});
