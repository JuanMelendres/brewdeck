import type { ReactNode } from 'react';
import { EmailVerificationGate } from '@/components/auth/EmailVerificationGate';
import { RequireAuth } from '@/components/auth/RequireAuth';
import { AppShell } from '@/components/layout/AppShell';

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <RequireAuth>
      <EmailVerificationGate>
        <AppShell>{children}</AppShell>
      </EmailVerificationGate>
    </RequireAuth>
  );
}
