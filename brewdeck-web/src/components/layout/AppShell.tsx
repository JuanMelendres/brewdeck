'use client';

import CoffeeMakerOutlinedIcon from '@mui/icons-material/CoffeeMakerOutlined';
import CoffeeOutlinedIcon from '@mui/icons-material/CoffeeOutlined';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import LocalCafeIcon from '@mui/icons-material/LocalCafe';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuIcon from '@mui/icons-material/Menu';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import SpaceDashboardOutlinedIcon from '@mui/icons-material/SpaceDashboardOutlined';
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined';
import AppBar from '@mui/material/AppBar';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import ButtonBase from '@mui/material/ButtonBase';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState, type ReactNode } from 'react';
import { EmailVerificationBanner } from '@/components/auth/EmailVerificationBanner';
import { useAuth } from '@/lib/auth/AuthProvider';
import { radius } from '@/lib/theme/tokens';

const DRAWER_WIDTH = 248;
const MOBILE_NAV_ID = 'mobile-navigation';

const NAV = [
  { label: 'Dashboard', href: '/dashboard', icon: <SpaceDashboardOutlinedIcon fontSize="small" /> },
  { label: 'Coffees', href: '/coffees', icon: <CoffeeOutlinedIcon fontSize="small" /> },
  { label: 'Recipes', href: '/recipes', icon: <MenuBookOutlinedIcon fontSize="small" /> },
  { label: 'Favorites', href: '/recipes/favorites', icon: <FavoriteBorderIcon fontSize="small" /> },
  { label: 'Brew Methods', href: '/brew-methods', icon: <CoffeeMakerOutlinedIcon fontSize="small" /> },
  { label: 'Brew Sessions', href: '/brew-sessions', icon: <TimerOutlinedIcon fontSize="small" /> },
];

const drawerPaperSx = {
  width: DRAWER_WIDTH,
  boxSizing: 'border-box',
  px: 2,
  py: 3,
  display: 'flex',
  flexDirection: 'column',
  gap: 3,
} as const;

/** The most specific nav entry for the path, so /recipes/favorites highlights Favorites, not Recipes. */
function activeHref(pathname: string): string | undefined {
  return NAV.map((item) => item.href)
    .filter((href) => pathname === href || pathname.startsWith(`${href}/`))
    .sort((a, b) => b.length - a.length)[0];
}

function Logo() {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
      <Box
        sx={{
          width: 36,
          height: 36,
          borderRadius: radius.control,
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
  );
}

/** Logo, links, and the user card: the same content in the desktop sidebar and the mobile drawer. */
function NavContent({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const current = activeHref(pathname);
  const name = user?.displayName ?? user?.email ?? '';
  const onLogout = () => {
    onNavigate?.();
    logout();
    router.replace('/login');
  };

  return (
    <>
      <Box sx={{ px: 1.5 }}>
        <Logo />
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
                onClick={onNavigate}
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
          borderRadius: radius.card,
          bgcolor: 'background.paper',
        }}
      >
        <ButtonBase
          component={Link}
          href="/account"
          aria-current={pathname === '/account' ? 'page' : undefined}
          onClick={onNavigate}
          sx={{ flexGrow: 1, minWidth: 0, justifyContent: 'flex-start', gap: 1.25, p: 0.5, borderRadius: radius.inner }}
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
    </>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const closeMobile = () => setMobileOpen(false);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* Desktop: a permanent sidebar from the md breakpoint up. */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: drawerPaperSx,
        }}
      >
        <NavContent />
      </Drawer>

      {/* Mobile: the same content in a drawer opened from the top bar; it closes on navigation. */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={closeMobile}
        sx={{ display: { xs: 'block', md: 'none' }, [`& .MuiDrawer-paper`]: drawerPaperSx }}
        slotProps={{ paper: { id: MOBILE_NAV_ID } }}
      >
        <NavContent onNavigate={closeMobile} />
      </Drawer>

      <Box sx={{ flexGrow: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <AppBar
          position="sticky"
          color="inherit"
          elevation={0}
          sx={{
            display: { xs: 'flex', md: 'none' },
            bgcolor: 'background.default',
            borderBottom: 1,
            borderColor: 'divider',
          }}
        >
          <Toolbar sx={{ gap: 1 }}>
            <IconButton
              edge="start"
              aria-label="Open navigation"
              aria-controls={MOBILE_NAV_ID}
              aria-expanded={mobileOpen}
              onClick={() => setMobileOpen(true)}
            >
              <MenuIcon />
            </IconButton>
            <Logo />
          </Toolbar>
        </AppBar>
        <Box component="main" sx={{ flexGrow: 1, minWidth: 0, px: { xs: 2, sm: 3, md: 5 }, py: { xs: 3, md: 4 } }}>
          <EmailVerificationBanner />
          {children}
        </Box>
      </Box>
    </Box>
  );
}
