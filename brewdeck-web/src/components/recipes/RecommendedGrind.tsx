'use client';

import Alert from '@mui/material/Alert';
import { useTranslations } from 'next-intl';
import { useRecipeBrewSessions } from '@/hooks/useRecipeBrewSessions';
import type { BrewSession } from '@/lib/api/types';
import { compareInstants } from '@/lib/format/dates';

type RatedWithGrind = BrewSession & { rating: number; actualGrind: string };

function bestRatedWithGrind(sessions: BrewSession[]): RatedWithGrind | null {
  const candidates = sessions.filter(
    (session): session is RatedWithGrind =>
      session.rating !== null && (session.actualGrind ?? '').trim() !== '',
  );
  if (candidates.length === 0) {
    return null;
  }
  return candidates.reduce((best, session) => {
    if (session.rating > best.rating) {
      return session;
    }
    if (session.rating === best.rating && compareInstants(session.brewedAt, best.brewedAt) > 0) {
      return session;
    }
    return best;
  });
}

export function RecommendedGrind({ recipeId }: { recipeId: number }) {
  const t = useTranslations('recipes.grind');
  const { data, isLoading } = useRecipeBrewSessions(recipeId);

  if (isLoading && !data) {
    return null;
  }

  const best = data ? bestRatedWithGrind(data.content) : null;

  if (!best) {
    return (
      <Alert severity="info" variant="outlined">
        {t('empty')}
      </Alert>
    );
  }

  return (
    <Alert severity="success" variant="outlined">
      {t.rich('recommended', {
        grind: best.actualGrind,
        rating: best.rating,
        strong: (chunks) => <strong>{chunks}</strong>,
      })}
    </Alert>
  );
}
