'use client';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Grid from '@mui/material/Grid';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { rateLimitMessage } from '@/lib/api/errors';
import { AiWaitHint } from './AiWaitHint';
import type { Recipe } from '@/lib/api/types';
import { ApiError } from '@/lib/api/client';
import { useRecipe, useRecipeStats } from '@/hooks/useRecipe';
import { useRecipeBrewSessions } from '@/hooks/useRecipeBrewSessions';
import { useImproveRecipe } from '@/hooks/useImproveRecipe';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { FeatureFlag } from '@/components/ui/FeatureFlag';
import { StatCard } from '@/components/dashboard/StatCard';
import { BrewSessionsTable } from '@/components/brew-sessions/BrewSessionsTable';
import { RecipeRatingTrend } from './RecipeRatingTrend';
import { RecommendedGrind } from './RecommendedGrind';
import { RecipeFormDialog } from './RecipeFormDialog';
import { ShareRecipeDialog } from './ShareRecipeDialog';
import { downloadRecipePdf, orDash } from '@/lib/pdf/recipePdf';
import { formatDate } from '@/lib/format/dates';

export function RecipeDetailView({ recipeId }: { recipeId: number }) {
  const t = useTranslations('recipes.detail');
  const tc = useTranslations('common');
  const locale = useLocale();
  const recipeQuery = useRecipe(recipeId);
  const statsQuery = useRecipeStats(recipeId);
  const historyQuery = useRecipeBrewSessions(recipeId);

  const improve = useImproveRecipe();
  const [improved, setImproved] = useState<{ recipe: Recipe; rationale: string } | null>(null);
  const [improveError, setImproveError] = useState<string | null>(null);
  const [pdfError, setPdfError] = useState<string | null>(null);
  const [shareOpen, setShareOpen] = useState(false);

  if (recipeQuery.isLoading && !recipeQuery.data) {
    return <Spinner />;
  }

  if (recipeQuery.isError || !recipeQuery.data) {
    return <ErrorState message={t('loadFailed')} onRetry={() => recipeQuery.refetch()} />;
  }

  const recipe = recipeQuery.data;

  const hasRatedHistory =
    historyQuery.data?.content.some((session) => session.rating !== null) ?? false;

  const onImprove = () => {
    setImproveError(null);
    improve.mutate(recipe.id, {
      onSuccess: (data) => {
        setImproved({
          recipe: {
            ...recipe,
            coffeeGrams: data.coffeeGrams ?? recipe.coffeeGrams,
            waterGrams: data.waterGrams ?? recipe.waterGrams,
            ratio: data.ratio ?? recipe.ratio,
            grindSetting: data.grindSetting ?? recipe.grindSetting,
            waterTemp: data.waterTemp ?? recipe.waterTemp,
            brewTime: data.brewTime ?? recipe.brewTime,
            steps: data.steps ?? recipe.steps,
            aiModel: data.aiModel,
            aiPromptVersion: data.aiPromptVersion,
          },
          rationale: data.rationale,
        });
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 422) {
          setImproveError(t('improveNeedsHistory'));
        } else {
          setImproveError(rateLimitMessage(error) ?? t('improveFailed'));
        }
      },
    });
  };

  const onExport = () => {
    setPdfError(null);
    try {
      downloadRecipePdf(recipe, locale);
    } catch {
      setPdfError(t('pdfFailed'));
    }
  };

  const details: Array<{ label: string; value: string }> = [
    { label: t('coffee'), value: recipe.coffeeName },
    { label: t('method'), value: recipe.methodName },
    { label: t('coffeeGrams'), value: orDash(recipe.coffeeGrams) },
    { label: t('waterGrams'), value: orDash(recipe.waterGrams) },
    { label: t('ratio'), value: orDash(recipe.ratio) },
    { label: t('grind'), value: orDash(recipe.grindSetting) },
    { label: t('waterTemp'), value: orDash(recipe.waterTemp) },
    { label: t('brewTime'), value: orDash(recipe.brewTime) },
  ];

  let statsBody: ReactNode;
  if (statsQuery.isLoading && !statsQuery.data) {
    statsBody = <Spinner />;
  } else if (statsQuery.isError || !statsQuery.data) {
    statsBody = (
      <ErrorState
        message={t('statsFailed')}
        onRetry={() => statsQuery.refetch()}
      />
    );
  } else {
    const stats = statsQuery.data;
    const cards: Array<{ label: string; value: string | number }> = [
      { label: t('totalSessions'), value: stats.totalSessions },
      {
        label: t('averageRating'),
        value: stats.averageRating === null ? '—' : stats.averageRating.toFixed(1),
      },
      { label: t('lastBrewed'), value: formatDate(stats.lastBrewedAt, undefined, locale) },
    ];
    statsBody = (
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid key={card.label} size={{ xs: 12, sm: 4 }}>
            <StatCard label={card.label} value={card.value} />
          </Grid>
        ))}
      </Grid>
    );
  }

  let historyBody: ReactNode;
  if (historyQuery.isLoading && !historyQuery.data) {
    historyBody = <Spinner />;
  } else if (historyQuery.isError || !historyQuery.data) {
    historyBody = (
      <ErrorState message={t('historyFailed')} onRetry={() => historyQuery.refetch()} />
    );
  } else if (historyQuery.data.content.length === 0) {
    historyBody = <EmptyState message={t('historyEmpty')} />;
  } else {
    historyBody = <BrewSessionsTable sessions={historyQuery.data.content} />;
  }

  return (
    <>
      <Button component={NextLink} href="/recipes" size="small" sx={{ mb: 1 }}>
        {t('back')}
      </Button>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Typography variant="h5" component="h1">
          {recipe.name}
        </Typography>
        {recipe.favorite ? <Chip label={tc('favorite')} color="primary" size="small" /> : null}
        {recipe.aiModel ? (
          <Chip
            label={t('aiSuggested', { model: recipe.aiModel })}
            title={recipe.aiPromptVersion ?? undefined}
            variant="outlined"
            size="small"
          />
        ) : null}
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <FeatureFlag name="aiRecipeAssistant">
          <Tooltip title={hasRatedHistory ? '' : t('improveDisabled')}>
            <span>
              <Button
                variant="outlined"
                size="small"
                disabled={!hasRatedHistory || improve.isPending}
                onClick={onImprove}
                startIcon={improve.isPending ? <CircularProgress size={16} /> : undefined}
              >
                {t('improve')}
              </Button>
            </span>
          </Tooltip>
        </FeatureFlag>
        <Button variant="outlined" size="small" onClick={onExport}>
          {t('exportPdf')}
        </Button>
        <Button variant="outlined" size="small" onClick={() => setShareOpen(true)}>
          {t('share')}
        </Button>
      </Box>
      <AiWaitHint active={improve.isPending} />
      {improveError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          {improveError}
        </Alert>
      ) : null}
      {pdfError ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          {pdfError}
        </Alert>
      ) : null}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {details.map((item) => (
          <Grid key={item.label} size={{ xs: 6, sm: 4, md: 3 }}>
            <StatCard label={item.label} value={item.value} />
          </Grid>
        ))}
      </Grid>

      {recipe.steps ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            {t('steps')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
            {recipe.steps}
          </Typography>
        </Box>
      ) : null}

      {recipe.expectedTaste ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            {t('expectedTaste')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {recipe.expectedTaste}
          </Typography>
        </Box>
      ) : null}

      <Box sx={{ mb: 3 }}>
        <RecommendedGrind recipeId={recipeId} />
      </Box>

      <Typography variant="h6" component="h2" gutterBottom>
        {t('statsTitle')}
      </Typography>
      {statsBody}

      <Box sx={{ mt: 3 }}>
        <RecipeRatingTrend recipeId={recipeId} />
      </Box>

      <Typography variant="h6" component="h2" gutterBottom sx={{ mt: 3 }}>
        {t('historyTitle')}
      </Typography>
      {historyBody}

      {improved ? (
        <RecipeFormDialog
          open
          recipe={improved.recipe}
          initialRationale={improved.rationale}
          onClose={() => setImproved(null)}
        />
      ) : null}
      <ShareRecipeDialog open={shareOpen} recipe={recipe} onClose={() => setShareOpen(false)} />
    </>
  );
}
