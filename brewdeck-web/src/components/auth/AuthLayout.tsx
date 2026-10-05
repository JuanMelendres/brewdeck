'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { BrandMark } from '@/components/ui/BrandMark';
import { Wordmark } from '@/components/ui/Wordmark';

/** Split screen for the public auth pages: brand panel on the left, the form in a card on the right. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', bgcolor: 'background.default' }}>
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flex: '0 0 44%',
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: 6,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
          <BrandMark size={28} />
          <Wordmark />
        </Box>
        <Box sx={{ maxWidth: 420 }}>
          <Typography variant="h3" component="p" sx={{ mb: 2 }}>
            Every cup, dialed in.
          </Typography>
          <Typography sx={{ opacity: 0.85 }}>
            Keep your coffees, recipes, and brew sessions in one place, and see what makes your best
            cup.
          </Typography>
        </Box>
        <Box />
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          px: 2,
          py: 6,
        }}
      >
        <Card sx={{ width: '100%', maxWidth: 440 }}>
          <CardContent sx={{ p: { xs: 3, sm: 4.5 }, '&:last-child': { pb: { xs: 3, sm: 4.5 } } }}>
            {children}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
