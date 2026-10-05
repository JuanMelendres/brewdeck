import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RequireAuth } from './RequireAuth';
import { renderWithTheme } from '@/test/renderWithTheme';

const replaceMock = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: replaceMock }) }));

const useAuthMock = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => useAuthMock() }));

describe('RequireAuth', () => {
  afterEach(() => vi.clearAllMocks());

  it('shows a spinner while loading', () => {
    useAuthMock.mockReturnValue({ status: 'loading' });
    renderWithTheme(
      <RequireAuth>
        <div>secret</div>
      </RequireAuth>,
    );
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('redirects to /login when anonymous', () => {
    useAuthMock.mockReturnValue({ status: 'anonymous' });
    renderWithTheme(
      <RequireAuth>
        <div>secret</div>
      </RequireAuth>,
    );
    expect(replaceMock).toHaveBeenCalledWith('/login');
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
  });

  it('renders children when authenticated', () => {
    useAuthMock.mockReturnValue({ status: 'authenticated' });
    renderWithTheme(
      <RequireAuth>
        <div>secret</div>
      </RequireAuth>,
    );
    expect(screen.getByText('secret')).toBeInTheDocument();
  });
});
