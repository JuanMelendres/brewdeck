import { render } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import type { ReactElement } from 'react';
// @ts-expect-error @/lib/theme/theme is created in Task 4
import { theme } from '@/lib/theme/theme';

export function renderWithTheme(ui: ReactElement) {
  return render(<ThemeProvider theme={theme}>{ui}</ThemeProvider>);
}
