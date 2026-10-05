'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useColorScheme } from '@mui/material/styles';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { useAuth } from '@/lib/auth/AuthProvider';
import { toThemePreference, type ThemeMode } from '@/lib/theme/themePreference';
import { ThemeChoice } from './ThemeChoice';

/**
 * One-time "Light or dark?" dialog for a user who has never chosen (`themePreference` is null).
 * It cannot be dismissed without choosing; Light is preselected, so it is one click. Picking an
 * option switches the app live behind the dialog. If saving fails, the theme stays applied here and
 * the dialog returns on the next sign-in, because the account value is still null.
 */
export function ThemeOnboardingDialog() {
  const t = useTranslations('theme.onboarding');
  const tc = useTranslations('common');
  const { user, updateTheme } = useAuth();
  const { mode, systemMode, setMode } = useColorScheme();
  const [saving, setSaving] = useState(false);
  const [failed, setFailed] = useState(false);
  const [closed, setClosed] = useState(false);
  const current: ThemeMode = (mode === 'system' ? systemMode : mode) ?? 'light';

  const open = user !== null && user.themePreference === null && !closed;

  const onContinue = async () => {
    if (failed) {
      setClosed(true);
      return;
    }
    setSaving(true);
    try {
      // Success updates the user, which closes the dialog.
      await updateTheme(toThemePreference(current));
    } catch {
      setFailed(true);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} aria-labelledby="theme-onboarding-title" maxWidth="xs" fullWidth>
      <DialogTitle id="theme-onboarding-title">{t('title')}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
        <DialogContentText>{t('body')}</DialogContentText>
        <ThemeChoice value={current} onChange={setMode} disabled={saving || failed} fullWidth />
        {failed ? (
          <Alert severity="error">{t('saveFailed')}</Alert>
        ) : null}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button variant="contained" onClick={onContinue} disabled={saving}>
          {tc('continue')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
