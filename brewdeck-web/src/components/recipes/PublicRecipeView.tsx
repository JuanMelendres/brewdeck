'use client';

import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { useTranslations } from 'next-intl';
import { usePublicRecipe } from '@/hooks/usePublicRecipe';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';

function orDash(value: string | number | null): string {
  return value === null || value === '' ? '—' : String(value);
}

export function PublicRecipeView({ token }: { token: string }) {
  const t = useTranslations('recipes.public');
  const td = useTranslations('recipes.detail');
  const query = usePublicRecipe(token);

  if (query.isLoading) {
    return <Spinner />;
  }

  if (query.isError || !query.data) {
    return <EmptyState message={t('unavailable')} />;
  }

  const recipe = query.data;
  const details: Array<{ label: string; value: string }> = [
    { label: td('coffee'), value: recipe.coffeeName },
    { label: td('method'), value: recipe.methodName },
    { label: td('coffeeGrams'), value: orDash(recipe.coffeeGrams) },
    { label: td('waterGrams'), value: orDash(recipe.waterGrams) },
    { label: td('ratio'), value: orDash(recipe.ratio) },
    { label: td('grind'), value: orDash(recipe.grindSetting) },
    { label: td('waterTemp'), value: orDash(recipe.waterTemp) },
    { label: td('brewTime'), value: orDash(recipe.brewTime) },
  ];

  let card: ReactNode = null;
  card = (
    <Paper sx={{ p: 3, maxWidth: 720, mx: 'auto', mt: 4 }}>
      <Typography variant="overline" color="text.secondary">
        {t('eyebrow')}
      </Typography>
      <Typography variant="h5" component="h1" gutterBottom>
        {recipe.name}
      </Typography>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {details.map((item) => (
          <Grid key={item.label} size={{ xs: 6, sm: 4, md: 3 }}>
            <Typography variant="caption" color="text.secondary">
              {item.label}
            </Typography>
            <Typography variant="body1">{item.value}</Typography>
          </Grid>
        ))}
      </Grid>

      {recipe.steps ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            {td('steps')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
            {recipe.steps}
          </Typography>
        </Box>
      ) : null}

      {recipe.expectedTaste ? (
        <Box>
          <Typography variant="subtitle1" gutterBottom>
            {td('expectedTaste')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {recipe.expectedTaste}
          </Typography>
        </Box>
      ) : null}
    </Paper>
  );

  return card;
}
