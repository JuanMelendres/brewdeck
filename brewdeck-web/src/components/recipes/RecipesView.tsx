'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useRecipes } from '@/hooks/useRecipes';
import { useDebounce } from '@/hooks/useDebounce';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { RecipeFilters } from './RecipeFilters';
import { RecipesTable } from './RecipesTable';
import { RecipeFormDialog } from './RecipeFormDialog';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import type { RecipeFilters as Filters, Recipe } from '@/lib/api/types';

export function RecipesView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
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
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load recipes." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No recipes found." />;
  } else {
    body = (
      <>
        <RecipesTable
          recipes={data.content}
          onEdit={(recipe) => setEditing(recipe)}
          onDelete={(recipe) => setDeleting(recipe)}
        />
        <TablePagination
          component="div"
          count={data.totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[10, 20, 50]}
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
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h5" component="h1">
          Recipes
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Add Recipe
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
