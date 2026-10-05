'use client';

import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useTranslations } from 'next-intl';
import { useRecipes } from '@/hooks/useRecipes';
import { useDebounce } from '@/hooks/useDebounce';
import { ErrorState } from '@/components/ui/ErrorState';
import { CardGridSkeleton } from '@/components/ui/CardGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { RecipeFilters } from './RecipeFilters';
import { RecipeCards } from './RecipeCards';
import { RecipeFormDialog } from './RecipeFormDialog';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import type { RecipeFilters as Filters, Recipe } from '@/lib/api/types';

export function RecipesView() {
  const t = useTranslations('recipes.list');
  const tc = useTranslations('common');
  const [page, setPage] = useState(0);
  // Multiples of 12 fill the 1-, 2-, and 3-column card grids evenly.
  const [size, setSize] = useState(12);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useRecipes({
    page,
    size,
    filters: debouncedFilters,
  });

  const handleFiltersChange = (next: Filters) => {
    setPage(0);
    setFilters(next);
  };

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Recipe | null>(null);
  const [deleting, setDeleting] = useState<Recipe | null>(null);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <CardGridSkeleton />;
  } else if (isError || !data) {
    body = <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    const filtered = Object.values(debouncedFilters).some((value) => value !== undefined && value !== '' && value !== false);
    body = filtered ? (
      <EmptyState message={t('noMatches')} />
    ) : (
      <EmptyState
        icon={<MenuBookOutlinedIcon />}
        message={t('empty')}
        action={{ label: t('addFirst'), onClick: () => setCreateOpen(true) }}
      />
    );
  } else {
    body = (
      <>
        <RecipeCards
          recipes={data.content}
          onEdit={(recipe) => setEditing(recipe)}
          onDelete={(recipe) => setDeleting(recipe)}
        />
        <TablePagination
          component="div"
          count={data.totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[12, 24, 48]}
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
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          {t('add')}
        </Button>
      </Box>
      <RecipeFilters value={filters} onChange={handleFiltersChange} />
      {body}

      {createOpen ? <RecipeFormDialog open onClose={() => setCreateOpen(false)} /> : null}
      {editing ? <RecipeFormDialog open recipe={editing} onClose={() => setEditing(null)} /> : null}
      {deleting ? <DeleteRecipeDialog open recipe={deleting} onClose={() => setDeleting(null)} /> : null}
    </>
  );
}
