'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import { useState } from 'react';
import { resendVerification } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';

export function EmailVerificationBanner() {
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
      onClose={() => setDismissed(true)}
      action={
        status === 'sent' ? undefined : (
          <Button color="inherit" size="small" onClick={onResend} disabled={status === 'sending'}>
            Resend link
          </Button>
        )
      }
      sx={{ mb: 2 }}
    >
      {status === 'sent'
        ? 'Verification email sent. Check your inbox.'
        : status === 'error'
          ? 'Could not resend the verification email. Please try again.'
          : 'Your email is not verified. Please check your inbox for the verification link.'}
    </Alert>
  );
}
