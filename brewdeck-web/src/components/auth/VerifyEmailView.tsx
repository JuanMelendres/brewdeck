'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useEffect, useRef, useState } from 'react';
import { verifyEmail } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';
import { Spinner } from '@/components/ui/Spinner';

export function VerifyEmailView() {
  const t = useTranslations('auth.verify');
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const { refreshUser } = useAuth();
  const [state, setState] = useState<'verifying' | 'success' | 'error'>(() =>
    token ? 'verifying' : 'error',
  );
  const started = useRef(false);

  useEffect(() => {
    if (started.current) {
      return;
    }
    started.current = true;
    if (!token) {
      return;
    }
    verifyEmail(token)
      .then(async () => {
        setState('success');
        await refreshUser();
      })
      .catch(() => setState('error'));
  }, [token, refreshUser]);

  return (
    <Stack spacing={3}>
      <Typography variant="h4" component="h1">
        {t('title')}
      </Typography>
      {state === 'verifying' ? <Spinner /> : null}
      {state === 'success' ? (
        <>
          <Alert severity="success">{t('success')}</Alert>
          <Button component={NextLink} href="/dashboard" variant="contained" size="large">
            {t('continue')}
          </Button>
        </>
      ) : null}
      {state === 'error' ? (
        <>
          <Alert severity="error">{t('invalidLink')}</Alert>
          <Typography variant="body2" color="text.secondary">
            {t('getNewLink')}
          </Typography>
        </>
      ) : null}
    </Stack>
  );
}
