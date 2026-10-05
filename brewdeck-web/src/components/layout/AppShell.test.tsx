import { screen, waitFor } from '@testing-library/react';
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
  it('shows the BrewDeck name next to the brand mark', () => {
    renderWithTheme(<AppShell>content</AppShell>);

    const [brew] = screen.getAllByText('Brew');
    expect(brew.parentElement).toHaveTextContent(/^BrewDeck$/);
    expect(brew.parentElement?.previousElementSibling?.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

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

  it('opens the mobile navigation from the top bar and closes it after navigating', async () => {
    renderWithTheme(<AppShell>content</AppShell>);
    const menuButton = screen.getByRole('button', { name: /open navigation/i });
    expect(menuButton).toHaveAttribute('aria-expanded', 'false');
    expect(document.getElementById('mobile-navigation')).not.toBeInTheDocument();

    await userEvent.click(menuButton);

    expect(menuButton).toHaveAttribute('aria-expanded', 'true');
    const drawer = document.getElementById('mobile-navigation');
    expect(drawer).toBeInTheDocument();
    // The open drawer is modal: the rest of the page is hidden from assistive tech, so the only
    // reachable Coffees link is the drawer's.
    const coffees = screen.getByRole('link', { name: 'Coffees' });
    expect(drawer).toContainElement(coffees);

    await userEvent.click(coffees);

    expect(menuButton).toHaveAttribute('aria-expanded', 'false');
    // The drawer unmounts its content after the exit transition.
    await waitFor(() => expect(document.getElementById('mobile-navigation')).not.toBeInTheDocument());
  });

  it('shows the user and logs out', async () => {
    renderWithTheme(<AppShell>content</AppShell>);

    expect(screen.getByRole('link', { name: /juan/i })).toHaveAttribute('href', '/account');
    await userEvent.click(screen.getByRole('button', { name: /log out/i }));

    expect(logout).toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith('/login');
  });
});
