'use client';

import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import FavoriteIcon from '@mui/icons-material/Favorite';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import IconButton from '@mui/material/IconButton';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useToggleFavorite } from '@/hooks/useRecipeMutations';
import { CardGrid, cardHoverSx } from '@/components/ui/CardGrid';
import type { Recipe } from '@/lib/api/types';
import { useNotify } from '@/lib/notifications/NotificationProvider';

/** The brewing parameters a card shows, in order; empty ones are left out. */
function parameters(recipe: Recipe): Array<{ label: string; value: string }> {
  const items: Array<{ label: string; value: string | null }> = [
    { label: 'Dose', value: recipe.coffeeGrams === null ? null : `${recipe.coffeeGrams} g` },
    { label: 'Water', value: recipe.waterGrams === null ? null : `${recipe.waterGrams} g` },
    { label: 'Ratio', value: recipe.ratio },
    { label: 'Temp', value: recipe.waterTemp === null ? null : `${recipe.waterTemp}°C` },
    { label: 'Grind', value: recipe.grindSetting },
    { label: 'Time', value: recipe.brewTime },
  ];
  return items.filter((item): item is { label: string; value: string } =>
    item.value !== null && item.value.trim() !== '',
  );
}

/** Recipes as a responsive card grid with a favorite toggle on each card. */
export function RecipeCards({
  recipes,
  onEdit,
  onDelete,
}: {
  recipes: Recipe[];
  onEdit?: (recipe: Recipe) => void;
  onDelete?: (recipe: Recipe) => void;
}) {
  return (
    <CardGrid
      items={recipes}
      getKey={(recipe) => recipe.id}
      renderItem={(recipe) => <RecipeCard recipe={recipe} onEdit={onEdit} onDelete={onDelete} />}
    />
  );
}

function RecipeCard({
  recipe,
  onEdit,
  onDelete,
}: {
  recipe: Recipe;
  onEdit?: (recipe: Recipe) => void;
  onDelete?: (recipe: Recipe) => void;
}) {
  const toggleFavorite = useToggleFavorite();
  const notify = useNotify();
  const onToggleFavorite = () => {
    const favorite = !recipe.favorite;
    toggleFavorite.mutate(
      { id: recipe.id, favorite },
      {
        onSuccess: () =>
          notify(favorite ? `Added "${recipe.name}" to favorites` : `Removed "${recipe.name}" from favorites`),
        onError: () => notify('Could not update the favorite. Try again.', 'error'),
      },
    );
  };
  const params = parameters(recipe);

  return (
    <Card component="article" sx={{ height: '100%', ...cardHoverSx }}>
      <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, p: 2.5, '&:last-child': { pb: 2.5 } }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 0.5 }}>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Typography variant="h6" component="h2" sx={{ fontSize: '1.125rem', lineHeight: 1.3 }}>
              <Link component={NextLink} href={`/recipes/${recipe.id}`} underline="hover" color="text.primary">
                {recipe.name}
              </Link>
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {recipe.coffeeName} · {recipe.methodName}
            </Typography>
          </Box>
          <IconButton
            size="small"
            aria-label={`Favorite ${recipe.name}`}
            aria-pressed={recipe.favorite}
            disabled={toggleFavorite.isPending}
            onClick={onToggleFavorite}
            sx={{ color: recipe.favorite ? 'secondary.main' : undefined }}
          >
            {recipe.favorite ? <FavoriteIcon fontSize="small" /> : <FavoriteBorderIcon fontSize="small" />}
          </IconButton>
          <IconButton size="small" aria-label={`Edit ${recipe.name}`} onClick={() => onEdit?.(recipe)}>
            <EditOutlinedIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" aria-label={`Delete ${recipe.name}`} onClick={() => onDelete?.(recipe)}>
            <DeleteOutlinedIcon fontSize="small" />
          </IconButton>
        </Box>

        {params.length > 0 ? (
          <Box
            sx={{
              mt: 'auto',
              display: 'grid',
              gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
              gap: 1,
            }}
          >
            {params.map((param) => (
              <Box key={param.label} sx={{ px: 1.25, py: 1, borderRadius: '10px', bgcolor: 'background.tint' }}>
                <Typography variant="caption" color="text.secondary" component="p">
                  {param.label}
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                  {param.value}
                </Typography>
              </Box>
            ))}
          </Box>
        ) : null}
      </CardContent>
    </Card>
  );
}
