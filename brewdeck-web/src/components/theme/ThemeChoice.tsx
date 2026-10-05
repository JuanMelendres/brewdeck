'use client';

import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import { useTranslations } from 'next-intl';
import type { ThemeMode } from '@/lib/theme/themePreference';

/** The Light/Dark pair shared by the Account setting and the first-login dialog. */
export function ThemeChoice({
  value,
  onChange,
  disabled,
  fullWidth,
}: {
  value: ThemeMode;
  onChange: (mode: ThemeMode) => void;
  disabled?: boolean;
  fullWidth?: boolean;
}) {
  const t = useTranslations('theme');
  return (
    <ToggleButtonGroup
      exclusive
      value={value}
      onChange={(_event, next: ThemeMode | null) => {
        if (next) onChange(next);
      }}
      aria-label={t('group')}
      disabled={disabled}
      fullWidth={fullWidth}
      sx={{ alignSelf: fullWidth ? 'stretch' : 'flex-start' }}
    >
      <ToggleButton value="light" sx={{ gap: 1, px: 2.5, py: 1.25, textTransform: 'none' }}>
        <LightModeOutlinedIcon fontSize="small" />
        {t('light')}
      </ToggleButton>
      <ToggleButton value="dark" sx={{ gap: 1, px: 2.5, py: 1.25, textTransform: 'none' }}>
        <DarkModeOutlinedIcon fontSize="small" />
        {t('dark')}
      </ToggleButton>
    </ToggleButtonGroup>
  );
}
