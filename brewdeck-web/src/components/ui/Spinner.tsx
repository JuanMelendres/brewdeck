'use client';

import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';

export function Spinner() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }} role="status" aria-label="Loading">
      <CircularProgress />
    </Box>
  );
}
