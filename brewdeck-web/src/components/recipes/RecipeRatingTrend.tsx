'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import { useTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useTranslations } from 'next-intl';
import { useRecipeBrewSessions } from '@/hooks/useRecipeBrewSessions';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { compareInstants, localDayKey } from '@/lib/format/dates';

type TrendPoint = { label: string; rating: number };

function toTrendPoints(
  sessions: Array<{ brewedAt: string; rating: number | null }>,
): TrendPoint[] {
  return sessions
    .filter((session): session is { brewedAt: string; rating: number } => session.rating !== null)
    .slice()
    .sort((a, b) => compareInstants(a.brewedAt, b.brewedAt))
    .map((session) => ({ label: localDayKey(session.brewedAt), rating: session.rating }));
}

export function RecipeRatingTrend({ recipeId }: { recipeId: number }) {
  const t = useTranslations('recipes.trend');
  const theme = useTheme();
  // CSS variable so the chart follows the active color scheme; plain palette value without one.
  const lineColor = (theme.vars ?? theme).palette.primary.main;
  const { data, isLoading, isError, refetch } = useRecipeBrewSessions(recipeId);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  } else {
    const points = toTrendPoints(data.content);
    if (points.length < 2) {
      body = (
        <Typography variant="body2" color="text.secondary">
          {t('notEnough')}
        </Typography>
      );
    } else {
      body = (
        <Box sx={{ width: '100%', height: 240 }}>
          <ResponsiveContainer>
            <LineChart data={points} margin={{ top: 8, right: 16, bottom: 8, left: -8 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" fontSize={12} />
              <YAxis domain={[0, 10]} allowDecimals={false} fontSize={12} />
              <Tooltip />
              <Line type="monotone" dataKey="rating" stroke={lineColor} strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </Box>
      );
    }
  }

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          {t('title')}
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
