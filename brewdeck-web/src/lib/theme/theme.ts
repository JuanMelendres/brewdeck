'use client';

import { createTheme } from '@mui/material/styles';

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
 * Decided in docs/product/spikes/ui-ux-refresh-spike.md (§16, Appendix A).
 */
export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#5B3A29', contrastText: '#FFF8EF' },
        secondary: { main: '#8A5A36', contrastText: '#FFF8EF' },
        background: { default: '#F6EFE6', paper: '#FFFBF6', sidebar: '#EFE4D6', tint: '#F1E3D3' },
        text: { primary: '#2B1D14', secondary: '#6B5646' },
        divider: '#E8DCCD',
      },
    },
    dark: {
      palette: {
        primary: { main: '#D4A55A', contrastText: '#1A130E' },
        secondary: { main: '#E2B86E', contrastText: '#1A130E' },
        background: { default: '#14100D', paper: '#1E1814', sidebar: '#0F0C0A', tint: '#251D17' },
        text: { primary: '#F3EAE0', secondary: '#A89888' },
        divider: '#2A221C',
      },
    },
  },
  shape: { borderRadius: 12 },
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
      styleOverrides: { root: { borderRadius: 12, paddingInline: 18 } },
    },
    MuiCard: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: 18,
          border: 'none',
          backgroundImage: 'none',
          boxShadow: '0 1px 2px rgba(43, 29, 20, 0.06), 0 8px 24px rgba(43, 29, 20, 0.05)',
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
      styleOverrides: { root: { borderRadius: 12 } },
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
          borderRadius: 12,
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
