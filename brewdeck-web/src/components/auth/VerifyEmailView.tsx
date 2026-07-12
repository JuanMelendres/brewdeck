'use client';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import { useSearchParams } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { verifyEmail } from '@/lib/api/auth';
import { useAuth } from '@/lib/auth/AuthProvider';
import { Spinner } from '@/components/ui/Spinner';

export function VerifyEmailView() {
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
    <Box sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      {state === 'verifying' ? <Spinner /> : null}
      {state === 'success' ? (
        <Alert severity="success">
          Your email has been verified. <a href="/dashboard">Continue to the app</a>
        </Alert>
      ) : null}
      {state === 'error' ? (
        <Alert severity="error">This verification link is invalid or has expired.</Alert>
      ) : null}
    </Box>
  );
}
