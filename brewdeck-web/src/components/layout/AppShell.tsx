'use client';

import CoffeeMakerOutlinedIcon from '@mui/icons-material/CoffeeMakerOutlined';
import CoffeeOutlinedIcon from '@mui/icons-material/CoffeeOutlined';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import LocalCafeIcon from '@mui/icons-material/LocalCafe';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import SpaceDashboardOutlinedIcon from '@mui/icons-material/SpaceDashboardOutlined';
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import ButtonBase from '@mui/material/ButtonBase';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import type { ReactNode } from 'react';
import { EmailVerificationBanner } from '@/components/auth/EmailVerificationBanner';
import { useAuth } from '@/lib/auth/AuthProvider';

const DRAWER_WIDTH = 248;

const NAV = [
  { label: 'Dashboard', href: '/dashboard', icon: <SpaceDashboardOutlinedIcon fontSize="small" /> },
  { label: 'Coffees', href: '/coffees', icon: <CoffeeOutlinedIcon fontSize="small" /> },
  { label: 'Recipes', href: '/recipes', icon: <MenuBookOutlinedIcon fontSize="small" /> },
  { label: 'Favorites', href: '/recipes/favorites', icon: <FavoriteBorderIcon fontSize="small" /> },
  { label: 'Brew Methods', href: '/brew-methods', icon: <CoffeeMakerOutlinedIcon fontSize="small" /> },
  { label: 'Brew Sessions', href: '/brew-sessions', icon: <TimerOutlinedIcon fontSize="small" /> },
];

/** The most specific nav entry for the path, so /recipes/favorites highlights Favorites, not Recipes. */
function activeHref(pathname: string): string | undefined {
  return NAV.map((item) => item.href)
    .filter((href) => pathname === href || pathname.startsWith(`${href}/`))
    .sort((a, b) => b.length - a.length)[0];
}

export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const current = activeHref(pathname);
  const name = user?.displayName ?? user?.email ?? '';
  const onLogout = () => {
    logout();
    router.replace('/login');
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            px: 2,
            py: 3,
            display: 'flex',
            flexDirection: 'column',
            gap: 3,
          },
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, px: 1.5 }}>
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: '12px',
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <LocalCafeIcon fontSize="small" />
          </Box>
          <Typography variant="h6" component="span" noWrap>
            BrewDeck
          </Typography>
        </Box>

        <nav aria-label="Main">
          <List disablePadding sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
            {NAV.map((item) => {
              const selected = item.href === current;
              return (
                <ListItemButton
                  key={item.href}
                  component={Link}
                  href={item.href}
                  selected={selected}
                  aria-current={selected ? 'page' : undefined}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText
                    primary={item.label}
                    slotProps={{ primary: { sx: { fontWeight: selected ? 600 : 500 } } }}
                  />
                </ListItemButton>
              );
            })}
          </List>
        </nav>

        <Box sx={{ flexGrow: 1 }} />

        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            p: 1,
            borderRadius: '14px',
            bgcolor: 'background.paper',
          }}
        >
          <ButtonBase
            component={Link}
            href="/account"
            aria-current={pathname === '/account' ? 'page' : undefined}
            sx={{ flexGrow: 1, minWidth: 0, justifyContent: 'flex-start', gap: 1.25, p: 0.5, borderRadius: '10px' }}
          >
            <Avatar sx={{ width: 34, height: 34, bgcolor: 'background.tint', color: 'text.primary', fontWeight: 600 }}>
              {name.charAt(0).toUpperCase()}
            </Avatar>
            <Box sx={{ minWidth: 0, textAlign: 'left' }}>
              <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>
                {name}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Account
              </Typography>
            </Box>
          </ButtonBase>
          <Tooltip title="Log out">
            <IconButton aria-label="Log out" onClick={onLogout} size="small">
              <LogoutIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, minWidth: 0, px: { xs: 3, md: 5 }, py: 4 }}>
        <EmailVerificationBanner />
        {children}
      </Box>
    </Box>
  );
}
