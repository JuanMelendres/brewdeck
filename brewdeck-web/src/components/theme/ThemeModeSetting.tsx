'use client';

import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import Stack from '@mui/material/Stack';
import { useColorScheme } from '@mui/material/styles';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Typography from '@mui/material/Typography';

type ThemeMode = 'light' | 'dark';

/**
 * Light/dark switch for the Account page. For now the choice lives in this browser only (MUI keeps it in
 * localStorage); the per-user backend preference and the first-login dialog come with workstream 7 of
 * docs/product/spikes/ui-ux-refresh-spike.md.
 */
export function ThemeModeSetting() {
  const { mode, systemMode, setMode } = useColorScheme();
  // `mode` is undefined until the provider mounts; "system" resolves to the OS mode.
  const current: ThemeMode = (mode === 'system' ? systemMode : mode) ?? 'light';

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" component="h2">
        Appearance
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Choose how BrewDeck looks on this device.
      </Typography>
      <ToggleButtonGroup
        exclusive
        value={current}
        onChange={(_event, next: ThemeMode | null) => {
          if (next) setMode(next);
        }}
        aria-label="Theme"
        sx={{ alignSelf: 'flex-start' }}
      >
        <ToggleButton value="light" sx={{ gap: 1, px: 2.5, textTransform: 'none' }}>
          <LightModeOutlinedIcon fontSize="small" />
          Light
        </ToggleButton>
        <ToggleButton value="dark" sx={{ gap: 1, px: 2.5, textTransform: 'none' }}>
          <DarkModeOutlinedIcon fontSize="small" />
          Dark
        </ToggleButton>
      </ToggleButtonGroup>
    </Stack>
  );
}
