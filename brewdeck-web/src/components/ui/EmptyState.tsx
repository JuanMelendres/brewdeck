'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { radius } from '@/lib/theme/tokens';

/**
 * Message for a list or widget with nothing to show. Pass `icon` and `action` for a full-page empty
 * list (for example "Add your first coffee"); widgets keep the plain one-line message.
 */
export function EmptyState({
  message,
  icon,
  action,
}: {
  message: string;
  icon?: ReactNode;
  action?: { label: string; onClick: () => void };
}) {
  if (!icon && !action) {
    return (
      <Typography variant="body1" color="text.secondary" sx={{ p: 4, textAlign: 'center' }}>
        {message}
      </Typography>
    );
  }

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
        py: 8,
        px: 2,
        textAlign: 'center',
      }}
    >
      {icon ? (
        <Box
          aria-hidden
          sx={{
            width: 64,
            height: 64,
            borderRadius: radius.round,
            bgcolor: 'background.tint',
            color: 'secondary.main',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            '& svg': { fontSize: 32 },
          }}
        >
          {icon}
        </Box>
      ) : null}
      <Typography variant="body1" color="text.secondary">
        {message}
      </Typography>
      {action ? (
        <Button variant="contained" onClick={action.onClick}>
          {action.label}
        </Button>
      ) : null}
    </Box>
  );
}
