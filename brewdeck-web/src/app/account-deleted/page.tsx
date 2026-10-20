import { AuthLayout } from '@/components/auth/AuthLayout';
import { AccountDeletedView } from '@/components/auth/AccountDeletedView';

export default function AccountDeletedPage() {
  return (
    <AuthLayout>
      <AccountDeletedView />
    </AuthLayout>
  );
}
