import { Suspense } from 'react';
import { AuthLayout } from '@/components/auth/AuthLayout';
import { EmailChangeLinkView } from '@/components/auth/EmailChangeLinkView';

export default function EmailChangeCancelPage() {
  return (
    <AuthLayout>
      <Suspense>
        <EmailChangeLinkView mode="cancel" />
      </Suspense>
    </AuthLayout>
  );
}
