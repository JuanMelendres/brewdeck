import { render } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import type { ReactElement } from 'react';
import { THEME_MODE_STORAGE_KEY, theme } from '@/lib/theme/theme';

export function renderWithTheme(ui: ReactElement) {
  return render(
    <ThemeProvider theme={theme} defaultMode="light" modeStorageKey={THEME_MODE_STORAGE_KEY}>
      {ui}
    </ThemeProvider>,
  );
}
