'use client';

import CoffeeOutlinedIcon from '@mui/icons-material/CoffeeOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useTranslations } from 'next-intl';
import { useBrewSessions } from '@/hooks/useBrewSessions';
import { useDebounce } from '@/hooks/useDebounce';
import { useCoffeeOptions, useRecipeOptions } from '@/hooks/useResourceOptions';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { CoffeeFormDialog } from '@/components/coffees/CoffeeFormDialog';
import { RecipeFormDialog } from '@/components/recipes/RecipeFormDialog';
import { BrewSessionFilters } from './BrewSessionFilters';
import { BrewSessionsTable } from './BrewSessionsTable';
import { BrewSessionFormDialog } from './BrewSessionFormDialog';
import type { BrewSessionFilters as Filters } from '@/lib/api/types';

export function BrewSessionsView() {
  const t = useTranslations('brewSessions.list');
  const tc = useTranslations('common');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useBrewSessions({
    page,
    size,
    filters: debouncedFilters,
  });

  const handleFiltersChange = (next: Filters) => {
    setPage(0);
    setFilters(next);
  };

  const [createOpen, setCreateOpen] = useState(false);

  // A brew session needs a recipe, and a recipe needs a coffee. Whatever is missing is created
  // first, and each form opens the next one with the new item selected.
  const coffeeOptions = useCoffeeOptions();
  const recipeOptions = useRecipeOptions();
  const needsCoffee = coffeeOptions.data?.length === 0;
  const needsRecipe = recipeOptions.data?.length === 0;
  const [coffeeOpen, setCoffeeOpen] = useState(false);
  const [recipeOpen, setRecipeOpen] = useState(false);
  const [newCoffeeId, setNewCoffeeId] = useState<number | undefined>(undefined);
  const [newRecipeId, setNewRecipeId] = useState<number | undefined>(undefined);

  const openRecipeForm = (coffeeId?: number) => {
    setNewCoffeeId(coffeeId);
    setRecipeOpen(true);
  };

  const openSessionForm = (recipeId?: number) => {
    setNewRecipeId(recipeId);
    setCreateOpen(true);
  };

  let body: ReactNode;
  if (isLoading && !data) {
    body = <TableSkeleton columns={6} />;
  } else if (isError || !data) {
    body = <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    const filtered = debouncedFilters.rating !== undefined;
    if (filtered) {
      body = <EmptyState message={t('noMatches')} />;
    } else if (needsRecipe && needsCoffee) {
      body = (
        <EmptyState
          icon={<CoffeeOutlinedIcon />}
          message={t('needsCoffee')}
          action={{ label: t('addCoffeeFirst'), onClick: () => setCoffeeOpen(true) }}
        />
      );
    } else if (needsRecipe) {
      body = (
        <EmptyState
          icon={<MenuBookOutlinedIcon />}
          message={t('needsRecipe')}
          action={{ label: t('addRecipeFirst'), onClick: () => openRecipeForm() }}
        />
      );
    } else {
      body = (
        <EmptyState
          icon={<TimerOutlinedIcon />}
          message={t('empty')}
          action={{ label: t('addFirst'), onClick: () => openSessionForm() }}
        />
      );
    }
  } else {
    body = (
      <>
        <BrewSessionsTable sessions={data.content} />
        <TablePagination
          component="div"
          count={data.totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[10, 20, 50]}
          labelRowsPerPage={tc('perPage')}
          sx={{ mt: 1 }}
          onPageChange={(_event, newPage) => setPage(newPage)}
          onRowsPerPageChange={(event) => {
            setSize(parseInt(event.target.value, 10));
            setPage(0);
          }}
        />
      </>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
        <Typography variant="h4" component="h1">
          {t('title')}
        </Typography>
        <Button variant="contained" disabled={needsRecipe} onClick={() => openSessionForm()}>
          {t('add')}
        </Button>
      </Box>
      <BrewSessionFilters value={filters} onChange={handleFiltersChange} />
      {body}

      {coffeeOpen ? (
        <CoffeeFormDialog
          open
          onClose={() => setCoffeeOpen(false)}
          onCreated={(coffee) => openRecipeForm(coffee.id)}
        />
      ) : null}
      {recipeOpen ? (
        <RecipeFormDialog
          open
          initialCoffeeId={newCoffeeId}
          onClose={() => setRecipeOpen(false)}
          onCreated={(recipe) => openSessionForm(recipe.id)}
        />
      ) : null}
      {createOpen ? (
        <BrewSessionFormDialog open recipeId={newRecipeId} onClose={() => setCreateOpen(false)} />
      ) : null}
    </>
  );
}
