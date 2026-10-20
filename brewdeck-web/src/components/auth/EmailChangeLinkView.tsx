'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { cancelEmailChange, confirmEmailChange } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';

type Mode = 'confirm' | 'cancel';

const ACTIONS: Record<Mode, (token: string) => Promise<void>> = {
  confirm: confirmEmailChange,
  cancel: cancelEmailChange,
};

/**
 * Landing page for the two email-change links (ADR-019). The action waits for a click instead of
 * running on load, so a mail scanner that opens the link cannot confirm or cancel the change. Both
 * actions end every session, so a signed-in browser is signed out locally too.
 */
export function EmailChangeLinkView({ mode }: { mode: Mode }) {
  const t = useTranslations(mode === 'confirm' ? 'auth.emailChangeConfirm' : 'auth.emailChangeCancel');
  const tl = useTranslations('auth.emailChangeLink');
  const token = useSearchParams().get('token') ?? '';
  const { status, logout } = useAuth();
  const [state, setState] = useState<'idle' | 'working' | 'done' | 'error'>(() =>
    token ? 'idle' : 'error',
  );

  const run = async () => {
    setState('working');
    try {
      await ACTIONS[mode](token);
      if (status === 'authenticated') {
        await logout();
      }
      setState('done');
    } catch {
      setState('error');
    }
  };

  return (
    <Stack spacing={3}>
      <Typography variant="h4" component="h1">
        {t('title')}
      </Typography>
      {state === 'idle' || state === 'working' ? (
        <>
          <Typography variant="body1">{t('intro')}</Typography>
          <Button
            variant="contained"
            size="large"
            color={mode === 'cancel' ? 'error' : 'primary'}
            onClick={run}
            disabled={state === 'working'}
          >
            {t('action')}
          </Button>
        </>
      ) : null}
      {state === 'done' ? (
        <>
          <Alert severity="success">{t('done')}</Alert>
          {mode === 'cancel' ? (
            <Button component={NextLink} href="/forgot-password" variant="contained" size="large">
              {t('resetPassword')}
            </Button>
          ) : null}
          <Button
            component={NextLink}
            href="/login"
            variant={mode === 'cancel' ? 'text' : 'contained'}
            size="large"
          >
            {tl('logIn')}
          </Button>
        </>
      ) : null}
      {state === 'error' ? <Alert severity="error">{tl('invalidLink')}</Alert> : null}
    </Stack>
  );
}
