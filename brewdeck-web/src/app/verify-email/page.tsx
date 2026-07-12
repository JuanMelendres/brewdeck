import { Suspense } from 'react';
import { VerifyEmailView } from '@/components/auth/VerifyEmailView';

export default function VerifyEmailPage() {
  return (
    <Suspense>
      <VerifyEmailView />
    </Suspense>
  );
}
