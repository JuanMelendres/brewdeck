'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useEffect, useState, type ReactNode } from 'react';
import { resendVerification } from '@/lib/api/auth';
import { rateLimitMessage } from '@/lib/api/errors';
import { useAuth } from '@/lib/auth/AuthProvider';
import { onEmailNotVerified } from '@/lib/auth/emailVerificationSignal';
import { useFeatureFlags } from '@/lib/featureFlags/FeatureFlagProvider';
import { Spinner } from '@/components/ui/Spinner';
import { AuthLayout } from './AuthLayout';

type ResendStatus = 'idle' | 'sending' | 'sent' | 'error';

/**
 * Blocks the app for an unverified account while the `requireEmailVerification` flag is on (the
 * backend enforces the same rule with 403 EMAIL_NOT_VERIFIED; this is the matching UX). Also
 * blocks as soon as the server returns that code, even if the cached flag still reads false.
 */
export function EmailVerificationGate({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const { flags, status } = useFeatureFlags();
  const [serverSaysUnverified, setServerSaysUnverified] = useState(false);

  useEffect(() => onEmailNotVerified(() => setServerSaysUnverified(true)), []);

  const unverified = user !== null && !user.emailVerified;
  if (!unverified) {
    return <>{children}</>;
  }
  if (flags.requireEmailVerification || serverSaysUnverified) {
    return <VerifyEmailRequired email={user.email} />;
  }
  // Don't flash the app for a moment before the flag arrives and blocks it.
  if (status === 'loading') {
    return <Spinner />;
  }
  return <>{children}</>;
}

function VerifyEmailRequired({ email }: { email: string }) {
  const { refreshUser, logout } = useAuth();
  const [resend, setResend] = useState<ResendStatus>('idle');
  const [resendError, setResendError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [stillUnverified, setStillUnverified] = useState(false);

  const onResend = async () => {
    setResend('sending');
    setResendError(null);
    try {
      await resendVerification();
      setResend('sent');
    } catch (error) {
      setResend('error');
      setResendError(
        rateLimitMessage(error) ?? 'Could not resend the verification email. Please try again.',
      );
    }
  };

  const onCheckAgain = async () => {
    setChecking(true);
    await refreshUser();
    // If the user is now verified, the gate re-renders the app and this component unmounts.
    setChecking(false);
    setStillUnverified(true);
  };

  return (
    <AuthLayout>
      <Typography variant="h4" component="h1" gutterBottom>
        Verify your email to continue
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        We sent a verification link to <strong>{email}</strong>. Open it to start using BrewDeck.
      </Typography>
      <Stack spacing={2}>
        {resend === 'sent' ? (
          <Alert severity="success">Verification email sent. Check your inbox.</Alert>
        ) : null}
        {resend === 'error' && resendError ? <Alert severity="error">{resendError}</Alert> : null}
        {stillUnverified ? (
          <Alert severity="info">
            Your email is not verified yet. Open the link in the email, then try again.
          </Alert>
        ) : null}
        <Button variant="contained" onClick={onResend} disabled={resend === 'sending'}>
          Resend verification email
        </Button>
        <Button variant="outlined" onClick={onCheckAgain} disabled={checking}>
          I&apos;ve verified my email
        </Button>
        <Button color="inherit" onClick={() => logout()}>
          Log out
        </Button>
      </Stack>
    </AuthLayout>
  );
}
