'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { resendVerification } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';

export function EmailVerificationBanner() {
  const t = useTranslations('auth.verifyBanner');
  const tv = useTranslations('auth.verifyRequired');
  const tc = useTranslations('common');
  const { user } = useAuth();
  const [dismissed, setDismissed] = useState(false);
  const [status, setStatus] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle');

  if (!user || user.emailVerified || dismissed) {
    return null;
  }

  const onResend = async () => {
    setStatus('sending');
    try {
      await resendVerification();
      setStatus('sent');
    } catch {
      setStatus('error');
    }
  };

  return (
    <Alert
      severity="warning"
      action={
        <>
          {status !== 'sent' && (
            <Button color="inherit" size="small" onClick={onResend} disabled={status === 'sending'}>
              {t('resend')}
            </Button>
          )}
          <Button color="inherit" size="small" onClick={() => setDismissed(true)}>
            {tc('dismiss')}
          </Button>
        </>
      }
      sx={{ mb: 2 }}
    >
      {status === 'sent'
        ? tv('resent')
        : status === 'error'
          ? tv('resendFailed')
          : t('notVerified')}
    </Alert>
  );
}
