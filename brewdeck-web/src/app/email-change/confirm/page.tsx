import { Suspense } from 'react';
import { AuthLayout } from '@/components/auth/AuthLayout';
import { EmailChangeLinkView } from '@/components/auth/EmailChangeLinkView';

export default function EmailChangeConfirmPage() {
  return (
    <AuthLayout>
      <Suspense>
        <EmailChangeLinkView mode="confirm" />
      </Suspense>
    </AuthLayout>
  );
}
