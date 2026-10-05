'use client';

import Box from '@mui/material/Box';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { ReactNode } from 'react';
import { useTranslations } from 'next-intl';
import { radius } from '@/lib/theme/tokens';

export type RankedItem = {
  key: string | number;
  label: string;
  href?: string;
  /** Secondary line under the label, such as "12 sessions". */
  caption?: ReactNode;
  /** Pill on the right with the value the list is ranked by. */
  badge: ReactNode;
};

/** Top-N ranking used by the dashboard widgets: rank circle, label (optionally a link), and a value pill. */
export function RankedList({ items }: { items: RankedItem[] }) {
  return (
    <Box
      component="ol"
      sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', flexDirection: 'column', gap: 0.5 }}
    >
      {items.map((item, index) => (
        <Box
          component="li"
          key={item.key}
          sx={{ display: 'flex', alignItems: 'center', gap: 1.75, py: 1.25, px: 0.5 }}
        >
          <Box
            aria-hidden
            sx={{
              width: 32,
              height: 32,
              flexShrink: 0,
              borderRadius: radius.round,
              bgcolor: 'background.tint',
              color: 'primary.main',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 600,
              fontSize: 13,
            }}
          >
            {index + 1}
          </Box>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            {item.href ? (
              <Link
                component={NextLink}
                href={item.href}
                underline="hover"
                color="text.primary"
                sx={{ fontWeight: 600 }}
              >
                {item.label}
              </Link>
            ) : (
              <Typography sx={{ fontWeight: 600 }}>{item.label}</Typography>
            )}
            {item.caption ? (
              <Typography variant="caption" color="text.secondary" component="p">
                {item.caption}
              </Typography>
            ) : null}
          </Box>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              flexShrink: 0,
              px: 1.25,
              py: 0.5,
              borderRadius: radius.pill,
              bgcolor: 'background.tint',
              color: 'secondary.main',
              fontWeight: 600,
              fontSize: 13,
            }}
          >
            {item.badge}
          </Box>
        </Box>
      ))}
    </Box>
  );
}

/** "1 session" / "9 sessions" (pluralized per language), with the number in its own element. */
export function Count({ value, kind }: { value: number; kind: 'sessions' | 'recipes' }) {
  const t = useTranslations('counts');
  return <>{t.rich(kind, { count: value, n: (chunks) => <span>{chunks}</span> })}</>;
}
