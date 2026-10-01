import type { ReactNode } from 'react';
import { EmailVerificationGate } from '@/components/auth/EmailVerificationGate';
import { RequireAuth } from '@/components/auth/RequireAuth';
import { AppShell } from '@/components/layout/AppShell';
import { ThemeOnboardingDialog } from '@/components/theme/ThemeOnboardingDialog';
import { ThemePreferenceSync } from '@/components/theme/ThemePreferenceSync';

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <RequireAuth>
      <EmailVerificationGate>
        <ThemePreferenceSync />
        <ThemeOnboardingDialog />
        <AppShell>{children}</AppShell>
      </EmailVerificationGate>
    </RequireAuth>
  );
}
