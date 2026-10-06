'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { radius } from '@/lib/theme/tokens';

export function StatCard({
  label,
  value,
  icon,
}: {
  label: string;
  value: string | number;
  icon?: ReactNode;
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1.75, p: 2.25, '&:last-child': { pb: 2.25 } }}>
        {icon ? (
          <Box
            aria-hidden
            sx={{
              width: 36,
              height: 36,
              borderRadius: radius.inner,
              bgcolor: 'background.tint',
              color: 'secondary.main',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {icon}
          </Box>
        ) : null}
        <Box>
          <Typography variant="h4" component="p" sx={{ lineHeight: 1.1 }}>
            {value}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {label}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
}
