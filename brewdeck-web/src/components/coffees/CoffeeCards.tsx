'use client';

import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PlaceOutlinedIcon from '@mui/icons-material/PlaceOutlined';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useTranslations } from 'next-intl';
import { CardGrid, cardHoverSx } from '@/components/ui/CardGrid';
import type { Coffee } from '@/lib/api/types';
import { radius } from '@/lib/theme/tokens';

const SCORES = ['acidityScore', 'bodyScore', 'sweetnessScore', 'bitternessScore'] as const;

function present(value: string | null): value is string {
  return value !== null && value.trim() !== '';
}

/** Coffees as a responsive card grid. */
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
    <CardGrid
      items={coffees}
      getKey={(coffee) => coffee.id}
      renderItem={(coffee) => <CoffeeCard coffee={coffee} onEdit={onEdit} onDelete={onDelete} />}
    />
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
  const t = useTranslations('coffees.fields');
  const tc = useTranslations('common');
  const place = [coffee.origin, coffee.region].filter(present).join(' · ');
  const chips = [coffee.roastLevel, coffee.process].filter(present);
  const scores = SCORES.flatMap((key) => {
    const value = coffee[key];
    return typeof value === 'number' ? [{ label: t(key), value }] : [];
  });

  return (
    <Card component="article" sx={{ height: '100%', ...cardHoverSx }}>
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
          <IconButton size="small" aria-label={tc('editItem', { name: coffee.name })} onClick={() => onEdit?.(coffee)}>
            <EditOutlinedIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" aria-label={tc('deleteItem', { name: coffee.name })} onClick={() => onDelete?.(coffee)}>
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
                    {tc('outOfFive', { value: score.value })}
                  </Typography>
                </Box>
                <Box aria-hidden sx={{ height: 6, borderRadius: radius.pill, bgcolor: 'background.tint', overflow: 'hidden' }}>
                  <Box sx={{ height: '100%', width: `${(score.value / 5) * 100}%`, bgcolor: 'secondary.main', borderRadius: radius.pill }} />
                </Box>
              </Box>
            ))}
          </Box>
        ) : null}
      </CardContent>
    </Card>
  );
}
