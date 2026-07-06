'use client';

import Alert from '@mui/material/Alert';
import { useRecipeBrewSessions } from '@/hooks/useRecipeBrewSessions';
import type { BrewSession } from '@/lib/api/types';

function bestRatedWithGrind(sessions: BrewSession[]): BrewSession | null {
  const candidates = sessions.filter(
    (session) => session.rating !== null && (session.actualGrind ?? '').trim() !== '',
  );
  if (candidates.length === 0) {
    return null;
  }
  return candidates.reduce((best, session) => {
    const rating = session.rating ?? 0;
    const bestRating = best.rating ?? 0;
    if (rating > bestRating) {
      return session;
    }
    if (rating === bestRating && session.brewedAt > best.brewedAt) {
      return session;
    }
    return best;
  });
}

export function RecommendedGrind({ recipeId }: { recipeId: number }) {
  const { data, isLoading } = useRecipeBrewSessions(recipeId);

  if (isLoading && !data) {
    return null;
  }

  const best = data ? bestRatedWithGrind(data.content) : null;

  if (!best) {
    return (
      <Alert severity="info" variant="outlined">
        Brew and rate a session to get a grind recommendation.
      </Alert>
    );
  }

  return (
    <Alert severity="success" variant="outlined">
      Recommended grind: <strong>{best.actualGrind}</strong> — from your best-rated session (
      {best.rating}/10).
    </Alert>
  );
}
