import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { AppShell } from './AppShell';

const pathname = vi.fn(() => '/dashboard');
const replace = vi.fn();
const logout = vi.fn();

vi.mock('next/navigation', () => ({
  usePathname: () => pathname(),
  useRouter: () => ({ replace }),
}));

vi.mock('@/lib/auth/AuthProvider', () => ({
  useAuth: () => ({ user: { displayName: 'Juan', email: 'juan@example.com' }, logout }),
}));

vi.mock('@/components/auth/EmailVerificationBanner', () => ({
  EmailVerificationBanner: () => null,
}));

beforeEach(() => vi.clearAllMocks());

describe('AppShell', () => {
  it('marks the current page in the navigation', () => {
    pathname.mockReturnValue('/coffees/7');
    renderWithTheme(<AppShell>content</AppShell>);

    expect(screen.getByRole('link', { name: 'Coffees' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
  });

  it('highlights Favorites, not Recipes, on the favorites page', () => {
    pathname.mockReturnValue('/recipes/favorites');
    renderWithTheme(<AppShell>content</AppShell>);

    expect(screen.getByRole('link', { name: 'Favorites' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Recipes' })).not.toHaveAttribute('aria-current');
  });

  it('shows the user and logs out', async () => {
    renderWithTheme(<AppShell>content</AppShell>);

    expect(screen.getByRole('link', { name: /juan/i })).toHaveAttribute('href', '/account');
    await userEvent.click(screen.getByRole('button', { name: /log out/i }));

    expect(logout).toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith('/login');
  });
});
