'use client';

import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { usePublicRecipe } from '@/hooks/usePublicRecipe';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';

function orDash(value: string | number | null): string {
  return value === null || value === '' ? '—' : String(value);
}

export function PublicRecipeView({ token }: { token: string }) {
  const query = usePublicRecipe(token);

  if (query.isLoading) {
    return <Spinner />;
  }

  if (query.isError || !query.data) {
    return <EmptyState message="This recipe isn't available." />;
  }

  const recipe = query.data;
  const details: Array<{ label: string; value: string }> = [
    { label: 'Coffee', value: recipe.coffeeName },
    { label: 'Method', value: recipe.methodName },
    { label: 'Coffee (g)', value: orDash(recipe.coffeeGrams) },
    { label: 'Water (g)', value: orDash(recipe.waterGrams) },
    { label: 'Ratio', value: orDash(recipe.ratio) },
    { label: 'Grind', value: orDash(recipe.grindSetting) },
    { label: 'Water Temp', value: orDash(recipe.waterTemp) },
    { label: 'Brew Time', value: orDash(recipe.brewTime) },
  ];

  let card: ReactNode = null;
  card = (
    <Paper sx={{ p: 3, maxWidth: 720, mx: 'auto', mt: 4 }}>
      <Typography variant="overline" color="text.secondary">
        BrewDeck · Shared recipe
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
            Steps
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
            {recipe.steps}
          </Typography>
        </Box>
      ) : null}

      {recipe.expectedTaste ? (
        <Box>
          <Typography variant="subtitle1" gutterBottom>
            Expected taste
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
