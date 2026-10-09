'use client';

import Typography from '@mui/material/Typography';
import { useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';

/** After this long, say the model is still working (ADR-016: local answers take 15–25 s, at most 90 s). */
export const LONG_WAIT_MS = 40_000;

/**
 * Tells the user what to expect while the AI works: a local model takes about 20 seconds, which
 * feels broken without a word. Announced politely to screen readers.
 */
export function AiWaitHint({ active }: { active: boolean }) {
  // Mounted only while active, so each request starts its own timer from zero.
  return active ? <WaitingMessage /> : null;
}

function WaitingMessage() {
  const t = useTranslations('recipes.ai');
  const [long, setLong] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setLong(true), LONG_WAIT_MS);
    return () => clearTimeout(timer);
  }, []);

  return (
    <Typography variant="body2" color="text.secondary" role="status" aria-live="polite">
      {long ? t('waitingLong') : t('waiting')}
    </Typography>
  );
}
