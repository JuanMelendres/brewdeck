'use client';

import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useFavoriteRecipes } from '@/hooks/useFavoriteRecipes';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { RecipesTable } from './RecipesTable';
import { RecipeFormDialog } from './RecipeFormDialog';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import type { Recipe } from '@/lib/api/types';

export function FavoriteRecipesView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const { data, isLoading, isError, refetch } = useFavoriteRecipes({ page, size });

  const [editing, setEditing] = useState<Recipe | null>(null);
  const [deleting, setDeleting] = useState<Recipe | null>(null);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load favorite recipes." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No favorite recipes yet." />;
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
      <Typography variant="h5" component="h1" gutterBottom>
        Favorite Recipes
      </Typography>
      {body}

      {editing ? (
        <RecipeFormDialog open recipe={editing} onClose={() => setEditing(null)} />
      ) : null}
      {deleting ? (
        <DeleteRecipeDialog open recipe={deleting} onClose={() => setDeleting(null)} />
      ) : null}
    </>
  );
}
