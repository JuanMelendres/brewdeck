'use client';

import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PlaceOutlinedIcon from '@mui/icons-material/PlaceOutlined';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import Link from '@mui/material/Link';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { Coffee } from '@/lib/api/types';

const SCORES: Array<{ key: keyof Coffee; label: string }> = [
  { key: 'acidityScore', label: 'Acidity' },
  { key: 'bodyScore', label: 'Body' },
  { key: 'sweetnessScore', label: 'Sweetness' },
  { key: 'bitternessScore', label: 'Bitterness' },
];

function present(value: string | null): value is string {
  return value !== null && value.trim() !== '';
}

const gridSize = { xs: 12, sm: 6, lg: 4 };

/** Coffees as a responsive card grid: one column on phones, two on tablets, three on desktop. */
export function CoffeeCards({
  coffees,
  onEdit,
  onDelete,
}: {
  coffees: Coffee[];
  onEdit?: (coffee: Coffee) => void;
  onDelete?: (coffee: Coffee) => void;
}) {
  return (
    <Grid container spacing={2.5} component="ul" sx={{ listStyle: 'none', p: 0, m: 0 }}>
      {coffees.map((coffee) => (
        <Grid key={coffee.id} size={gridSize} component="li">
          <CoffeeCard coffee={coffee} onEdit={onEdit} onDelete={onDelete} />
        </Grid>
      ))}
    </Grid>
  );
}

function CoffeeCard({
  coffee,
  onEdit,
  onDelete,
}: {
  coffee: Coffee;
  onEdit?: (coffee: Coffee) => void;
  onDelete?: (coffee: Coffee) => void;
}) {
  const place = [coffee.origin, coffee.region].filter(present).join(' · ');
  const chips = [coffee.roastLevel, coffee.process].filter(present);
  const scores = SCORES.flatMap(({ key, label }) => {
    const value = coffee[key];
    return typeof value === 'number' ? [{ label, value }] : [];
  });

  return (
    <Card component="article" sx={{ height: '100%', transition: 'box-shadow 150ms ease' }}>
      <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, p: 2.5, '&:last-child': { pb: 2.5 } }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Typography variant="h6" component="h2" sx={{ fontSize: '1.125rem', lineHeight: 1.3 }}>
              <Link component={NextLink} href={`/coffees/${coffee.id}`} underline="hover" color="text.primary">
                {coffee.name}
              </Link>
            </Typography>
            {present(coffee.brand) ? (
              <Typography variant="body2" color="text.secondary">
                {coffee.brand}
              </Typography>
            ) : null}
          </Box>
          <IconButton size="small" aria-label={`Edit ${coffee.name}`} onClick={() => onEdit?.(coffee)}>
            <EditOutlinedIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" aria-label={`Delete ${coffee.name}`} onClick={() => onDelete?.(coffee)}>
            <DeleteOutlinedIcon fontSize="small" />
          </IconButton>
        </Box>

        {place ? (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, color: 'text.secondary' }}>
            <PlaceOutlinedIcon sx={{ fontSize: 16 }} aria-hidden />
            <Typography variant="body2">{place}</Typography>
          </Box>
        ) : null}

        {chips.length > 0 ? (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
            {chips.map((chip) => (
              <Chip key={chip} label={chip} size="small" sx={{ bgcolor: 'background.tint', color: 'text.primary' }} />
            ))}
          </Box>
        ) : null}

        {present(coffee.notesPrimary) ? (
          <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
            {coffee.notesPrimary}
          </Typography>
        ) : null}

        {scores.length > 0 ? (
          <Box
            sx={{ mt: 'auto', display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 1.25, columnGap: 2 }}
          >
            {scores.map((score) => (
              <Box key={score.label}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="caption" color="text.secondary">
                    {score.label}
                  </Typography>
                  <Typography variant="caption" sx={{ fontWeight: 600 }}>
                    {score.value}/5
                  </Typography>
                </Box>
                <Box aria-hidden sx={{ height: 6, borderRadius: 999, bgcolor: 'background.tint', overflow: 'hidden' }}>
                  <Box sx={{ height: '100%', width: `${(score.value / 5) * 100}%`, bgcolor: 'secondary.main', borderRadius: 999 }} />
                </Box>
              </Box>
            ))}
          </Box>
        ) : null}
      </CardContent>
    </Card>
  );
}

/** Placeholder grid shown while the first page loads. */
export function CoffeeCardsSkeleton({ count = 6 }: { count?: number }) {
  return (
    <Box role="status" aria-label="Loading">
      <Grid container spacing={2.5}>
        {Array.from({ length: count }, (_, index) => (
          <Grid key={index} size={gridSize}>
            <Card sx={{ height: '100%' }}>
              <CardContent sx={{ p: 2.5 }}>
                <Skeleton variant="text" width="60%" height={28} />
                <Skeleton variant="text" width="35%" />
                <Skeleton variant="text" width="50%" sx={{ mt: 1 }} />
                <Box sx={{ display: 'flex', gap: 1, my: 1.5 }}>
                  <Skeleton variant="rounded" width={64} height={24} />
                  <Skeleton variant="rounded" width={64} height={24} />
                </Box>
                <Skeleton variant="rounded" height={44} />
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
