'use client';

import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useFavoriteRecipes } from '@/hooks/useFavoriteRecipes';
import { ErrorState } from '@/components/ui/ErrorState';
import { CardGridSkeleton } from '@/components/ui/CardGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { RecipeCards } from './RecipeCards';
import { RecipeFormDialog } from './RecipeFormDialog';
import { DeleteRecipeDialog } from './DeleteRecipeDialog';
import type { Recipe } from '@/lib/api/types';

export function FavoriteRecipesView() {
  const [page, setPage] = useState(0);
  // Multiples of 12 fill the 1-, 2-, and 3-column card grids evenly.
  const [size, setSize] = useState(12);

  const { data, isLoading, isError, refetch } = useFavoriteRecipes({ page, size });

  const [editing, setEditing] = useState<Recipe | null>(null);
  const [deleting, setDeleting] = useState<Recipe | null>(null);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <CardGridSkeleton />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load favorite recipes." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = (
      <EmptyState
        icon={<FavoriteBorderIcon />}
        message="No favorite recipes yet. Tap the heart on a recipe to keep it here."
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
          labelRowsPerPage="Per page"
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
      <Typography variant="h4" component="h1" sx={{ mb: 2 }}>
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
