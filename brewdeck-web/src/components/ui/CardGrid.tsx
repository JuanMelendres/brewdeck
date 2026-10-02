'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Skeleton from '@mui/material/Skeleton';
import type { ReactNode } from 'react';

/** One column on phones, two from sm, three from lg. Page sizes of 12, 24, and 48 fill it evenly. */
const itemSize = { xs: 12, sm: 6, lg: 4 };

/** A list of cards in a responsive grid; each item becomes a list item. */
export function CardGrid<T>({
  items,
  getKey,
  renderItem,
}: {
  items: T[];
  getKey: (item: T) => string | number;
  renderItem: (item: T) => ReactNode;
}) {
  return (
    <Grid container spacing={2.5} component="ul" sx={{ listStyle: 'none', p: 0, m: 0 }}>
      {items.map((item) => (
        <Grid key={getKey(item)} size={itemSize} component="li">
          {renderItem(item)}
        </Grid>
      ))}
    </Grid>
  );
}

/** Placeholder cards shown while the first page of a card grid loads. */
export function CardGridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <Box role="status" aria-label="Loading">
      <Grid container spacing={2.5}>
        {Array.from({ length: count }, (_, index) => (
          <Grid key={index} size={itemSize}>
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
