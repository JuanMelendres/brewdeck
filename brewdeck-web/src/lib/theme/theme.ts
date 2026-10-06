'use client';

import { createTheme } from '@mui/material/styles';
import { baseRadius, elevation, palettes, radius } from './tokens';

declare module '@mui/material/styles' {
  interface TypeBackground {
    /** Navigation drawer surface. */
    sidebar: string;
    /** Soft tinted fill for icon badges, rank chips, and progress tracks. */
    tint: string;
  }
}

/** localStorage key MUI uses for the selected mode; shared with `InitColorSchemeScript`. */
export const THEME_MODE_STORAGE_KEY = 'brewdeck-theme-mode';

// Font families come from CSS variables set by `next/font` in the root layout,
// so the theme stays importable in tests (where those variables are absent).
const bodyFont = 'var(--font-body), "Helvetica Neue", Arial, sans-serif';
const displayFont = 'var(--font-display), Georgia, "Times New Roman", serif';

const heading = { fontFamily: displayFont, fontWeight: 600, letterSpacing: '-0.01em' };

/**
 * Light mode = "warm café" (direction A), dark mode = "dark premium" (direction C).
 * Decided in docs/product/spikes/ui-ux-refresh-spike.md (§16); values live in ./tokens.ts.
 */
export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  colorSchemes: {
    light: { palette: palettes.light },
    dark: { palette: palettes.dark },
  },
  shape: { borderRadius: baseRadius },
  typography: {
    fontFamily: bodyFont,
    h1: heading,
    h2: heading,
    h3: heading,
    h4: heading,
    h5: heading,
    h6: heading,
    button: { fontWeight: 600, textTransform: 'none' },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: { body: { WebkitFontSmoothing: 'antialiased' } },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { borderRadius: radius.control, paddingInline: 18 } },
    },
    MuiCard: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: radius.card,
          border: 'none',
          backgroundImage: 'none',
          boxShadow: elevation.card,
          ...theme.applyStyles('dark', {
            border: `1px solid ${theme.vars.palette.divider}`,
            boxShadow: 'none',
          }),
        }),
      },
    },
    MuiPaper: {
      styleOverrides: { outlined: ({ theme }) => ({ borderColor: theme.vars.palette.divider }) },
    },
    MuiTableCell: {
      styleOverrides: {
        root: ({ theme }) => ({ borderColor: theme.vars.palette.divider }),
        head: ({ theme }) => ({
          fontWeight: 600,
          fontSize: '0.8125rem',
          color: theme.vars.palette.text.secondary,
        }),
      },
    },
    MuiChip: {
      styleOverrides: { root: { fontWeight: 600 } },
    },
    MuiOutlinedInput: {
      styleOverrides: { root: { borderRadius: radius.control } },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: ({ theme }) => ({
          backgroundColor: theme.vars.palette.background.sidebar,
          borderRight: 'none',
          ...theme.applyStyles('dark', {
            borderRight: `1px solid ${theme.vars.palette.divider}`,
          }),
        }),
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: radius.control,
          '&.Mui-selected, &.Mui-selected:hover': {
            backgroundColor: theme.vars.palette.primary.main,
            color: theme.vars.palette.primary.contrastText,
            '& .MuiListItemIcon-root': { color: 'inherit' },
          },
          ...theme.applyStyles('dark', {
            '&.Mui-selected, &.Mui-selected:hover': {
              backgroundColor: theme.vars.palette.background.tint,
              color: theme.vars.palette.text.primary,
              '& .MuiListItemIcon-root': { color: theme.vars.palette.primary.main },
            },
          }),
        }),
      },
    },
    MuiListItemIcon: {
      styleOverrides: { root: { minWidth: 36, color: 'inherit' } },
    },
  },
});
