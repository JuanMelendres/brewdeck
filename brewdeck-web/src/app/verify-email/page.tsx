import { Suspense } from 'react';
import { AuthLayout } from '@/components/auth/AuthLayout';
import { VerifyEmailView } from '@/components/auth/VerifyEmailView';

export default function VerifyEmailPage() {
  return (
    <AuthLayout>
      <Suspense>
        <VerifyEmailView />
      </Suspense>
    </AuthLayout>
  );
}
