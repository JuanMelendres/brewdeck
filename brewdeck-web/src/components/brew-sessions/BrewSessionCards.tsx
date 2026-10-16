'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import type { BrewSession } from '@/lib/api/types';
import { formatDateTime } from '@/lib/format/dates';
import { RatingPill, TasteCell } from './BrewSessionsTable';

/** Brew sessions as stacked cards, for phones where the table would scroll sideways. */
export function BrewSessionCards({ sessions }: { sessions: BrewSession[] }) {
  const t = useTranslations('brewSessions.list');
  const tc = useTranslations('common');
  const locale = useLocale();
  return (
    <Box component="ul" sx={{ listStyle: 'none', p: 0, m: 0, display: 'grid', gap: 1.5 }}>
      {sessions.map((session) => {
        const brewed = formatDateTime(session.brewedAt, undefined, locale);
        const params = [
          session.actualTemp === null ? null : tc('celsius', { value: session.actualTemp }),
          session.actualTime,
        ].filter((value): value is string => value !== null && value.trim() !== '');
        return (
          <Card key={session.id} component="li" variant="outlined">
            <CardContent sx={{ display: 'grid', gap: 1, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
                <Box sx={{ minWidth: 0 }}>
                  <Link
                    component={NextLink}
                    href={`/brew-sessions/${session.id}`}
                    underline="hover"
                    color="text.primary"
                    aria-label={t('viewSession', { date: brewed })}
                    sx={{ fontWeight: 600 }}
                  >
                    {session.recipeName}
                  </Link>
                  <Typography variant="body2" color="text.secondary">
                    {brewed}
                    {params.length > 0 ? ` · ${params.join(' · ')}` : ''}
                  </Typography>
                </Box>
                <RatingPill rating={session.rating} />
              </Box>
              {session.tasteResult ? <TasteCell text={session.tasteResult} /> : null}
            </CardContent>
          </Card>
        );
      })}
    </Box>
  );
}
