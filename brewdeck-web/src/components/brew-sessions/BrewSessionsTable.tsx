'use client';

import StarRoundedIcon from '@mui/icons-material/StarRounded';
import Box from '@mui/material/Box';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import { useState } from 'react';
import type { BrewSession } from '@/lib/api/types';
import { formatDateTime } from '@/lib/format/dates';
import { radius } from '@/lib/theme/tokens';

export function Muted({ children }: { children: string }) {
  return (
    <Typography component="span" variant="body2" color="text.secondary">
      {children}
    </Typography>
  );
}

function orMuted(value: string | number | null) {
  return value === null || value === '' ? <Muted>—</Muted> : value;
}

/** Longer notes start collapsed to two lines, with a toggle to read them in full. */
const TASTE_PREVIEW_CHARS = 80;

export function TasteCell({ text }: { text: string }) {
  const t = useTranslations('brewSessions.list');
  const [expanded, setExpanded] = useState(false);
  const long = text.length > TASTE_PREVIEW_CHARS;
  const collapsed = long && !expanded;
  return (
    <>
      <Typography
        variant="body2"
        sx={{
          whiteSpace: 'pre-wrap',
          overflowWrap: 'anywhere',
          ...(collapsed && {
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }),
        }}
      >
        {text}
      </Typography>
      {long && (
        <Link
          component="button"
          type="button"
          variant="body2"
          underline="hover"
          aria-expanded={expanded}
          onClick={() => setExpanded((value) => !value)}
          sx={{ mt: 0.5, fontWeight: 600 }}
        >
          {expanded ? t('showLess') : t('showMore')}
        </Link>
      )}
    </>
  );
}

/** "9/10" with a star, or a dash when the brew was not rated. */
export function RatingPill({ rating }: { rating: number | null }) {
  if (rating === null) {
    return <Muted>—</Muted>;
  }
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.5,
        px: 1.25,
        py: 0.25,
        borderRadius: radius.pill,
        bgcolor: 'background.tint',
        color: 'secondary.main',
        fontWeight: 600,
      }}
    >
      <StarRoundedIcon sx={{ fontSize: 15 }} aria-hidden />
      <span>{rating}</span>
      <Typography component="span" variant="caption" color="text.secondary">
        /10
      </Typography>
    </Box>
  );
}

export function BrewSessionsTable({ sessions }: { sessions: BrewSession[] }) {
  const t = useTranslations('brewSessions.list');
  const tc = useTranslations('common');
  const locale = useLocale();
  return (
    <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: radius.card }}>
      <Table sx={{ minWidth: 720, '& td, & th': { px: 2.5 }, '& td': { py: 1.75 } }}>
        <TableHead>
          <TableRow>
            <TableCell>{t('brewed')}</TableCell>
            <TableCell>{t('recipe')}</TableCell>
            <TableCell>{t('rating')}</TableCell>
            <TableCell>{t('temp')}</TableCell>
            <TableCell>{t('time')}</TableCell>
            <TableCell>{t('taste')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sessions.map((session) => (
            <TableRow key={session.id} hover>
              <TableCell sx={{ whiteSpace: 'nowrap' }}>
                <Link
                  component={NextLink}
                  href={`/brew-sessions/${session.id}`}
                  underline="hover"
                  color="text.primary"
                  aria-label={t('viewSession', { date: formatDateTime(session.brewedAt, undefined, locale) })}
                >
                  {formatDateTime(session.brewedAt, undefined, locale)}
                </Link>
              </TableCell>
              <TableCell>
                <Link
                  component={NextLink}
                  href={`/recipes/${session.recipeId}`}
                  underline="hover"
                  color="text.primary"
                  sx={{ fontWeight: 600 }}
                >
                  {session.recipeName}
                </Link>
              </TableCell>
              <TableCell>
                <RatingPill rating={session.rating} />
              </TableCell>
              <TableCell>{session.actualTemp === null ? <Muted>—</Muted> : tc('celsius', { value: session.actualTemp })}</TableCell>
              <TableCell>{orMuted(session.actualTime)}</TableCell>
              <TableCell sx={{ maxWidth: 280 }}>
                {session.tasteResult ? (
                  <TasteCell text={session.tasteResult} />
                ) : (
                  <Muted>—</Muted>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
