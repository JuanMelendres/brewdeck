'use client';

import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Link from 'next/link';
import type { ReactNode } from 'react';

const DRAWER_WIDTH = 220;

const NAV = [
  { label: 'Dashboard', href: '/dashboard', enabled: true },
  { label: 'Coffees', href: '/coffees', enabled: true },
  { label: 'Recipes', href: '/recipes', enabled: false },
  { label: 'Brew Sessions', href: '/brew-sessions', enabled: false },
];

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <Typography variant="h6" noWrap>
            BrewDeck
          </Typography>
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
        {children}
      </Box>
    </Box>
  );
}
