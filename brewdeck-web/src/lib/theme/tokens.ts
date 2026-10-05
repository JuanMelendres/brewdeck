/**
 * Design tokens shared by the MUI theme and component `sx` props.
 * Architecture and rules: docs/architecture/design-foundation-tdd.md (ADR-014).
 */

type SchemePalette = {
  primary: { main: string; contrastText: string };
  secondary: { main: string; contrastText: string };
  background: { default: string; paper: string; sidebar: string; tint: string };
  text: { primary: string; secondary: string };
  divider: string;
};

/** Light = "warm café" (direction A), dark = "dark premium" (direction C). */
export const palettes = {
  light: {
    primary: { main: '#5B3A29', contrastText: '#FFF8EF' },
    secondary: { main: '#8A5A36', contrastText: '#FFF8EF' },
    background: { default: '#F6EFE6', paper: '#FFFBF6', sidebar: '#EFE4D6', tint: '#F1E3D3' },
    text: { primary: '#2B1D14', secondary: '#6B5646' },
    divider: '#E8DCCD',
  },
  dark: {
    primary: { main: '#D4A55A', contrastText: '#1A130E' },
    secondary: { main: '#E2B86E', contrastText: '#1A130E' },
    background: { default: '#14100D', paper: '#1E1814', sidebar: '#0F0C0A', tint: '#251D17' },
    text: { primary: '#F3EAE0', secondary: '#A89888' },
    divider: '#2A221C',
  },
} as const satisfies Record<'light' | 'dark', SchemePalette>;

/** `theme.shape.borderRadius`, in px: MUI's default radius and the `sx` spacing unit for radii. */
export const baseRadius = 12;

/**
 * Corner radii as px strings: a bare number in `sx.borderRadius` is multiplied by
 * `shape.borderRadius`, so `1.5` would mean 18 px.
 */
export const radius = {
  /** Small inner elements: icon badges, parameter tiles. */
  inner: '10px',
  /** Buttons, inputs, nav items. */
  control: `${baseRadius}px`,
  /** Cards, table containers, and other top-level surfaces. */
  card: '18px',
  pill: '999px',
  round: '50%',
} as const;

/** Light-mode shadows; dark mode uses borders instead. */
export const elevation = {
  card: '0 1px 2px rgba(43, 29, 20, 0.06), 0 8px 24px rgba(43, 29, 20, 0.05)',
  cardHover: '0 2px 4px rgba(43, 29, 20, 0.08), 0 12px 28px rgba(43, 29, 20, 0.1)',
} as const;

export const motion = {
  fast: '150ms',
  easing: 'ease',
} as const;
