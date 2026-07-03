'use client';

import Typography from '@mui/material/Typography';

export function EmptyState({ message }: { message: string }) {
  return (
    <Typography variant="body1" color="text.secondary" sx={{ p: 4, textAlign: 'center' }}>
      {message}
    </Typography>
  );
}
