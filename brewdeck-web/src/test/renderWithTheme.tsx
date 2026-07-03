import { render } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import type { ReactElement } from 'react';
import { theme } from '@/lib/theme/theme';

export function renderWithTheme(ui: ReactElement) {
  return render(<ThemeProvider theme={theme}>{ui}</ThemeProvider>);
}
