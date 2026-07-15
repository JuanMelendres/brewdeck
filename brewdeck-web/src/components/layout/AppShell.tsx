'use client';

import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import type { ReactNode } from 'react';
import { EmailVerificationBanner } from '@/components/auth/EmailVerificationBanner';
import { useAuth } from '@/lib/auth/AuthProvider';

const DRAWER_WIDTH = 220;

const NAV = [
  { label: 'Dashboard', href: '/dashboard', enabled: true },
  { label: 'Coffees', href: '/coffees', enabled: true },
  { label: 'Recipes', href: '/recipes', enabled: true },
  { label: 'Favorites', href: '/recipes/favorites', enabled: true },
  { label: 'Brew Methods', href: '/brew-methods', enabled: true },
  { label: 'Brew Sessions', href: '/brew-sessions', enabled: true },
  { label: 'Account', href: '/account', enabled: true },
];

export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const onLogout = () => {
    logout();
    router.replace('/login');
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Typography variant="h6" noWrap>
            BrewDeck
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {user ? (
              <Typography
                variant="body2"
                noWrap
                component={Link}
                href="/account"
                sx={{ color: 'inherit', textDecoration: 'none' }}
              >
                {user.displayName ?? user.email}
              </Typography>
            ) : null}
            <Button color="inherit" onClick={onLogout}>
              Logout
            </Button>
          </Box>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <List>
          {NAV.map((item) =>
            item.enabled ? (
              <ListItemButton key={item.href} component={Link} href={item.href}>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ) : (
              <ListItemButton key={item.href} disabled>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ),
          )}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        <EmailVerificationBanner />
        {children}
      </Box>
    </Box>
  );
}
