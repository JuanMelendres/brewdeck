'use client';

import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { useState } from 'react';
import { useBrewSession } from '@/hooks/useBrewSession';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { StatCard } from '@/components/dashboard/StatCard';
import { formatDateTime } from '@/lib/format/dates';
import { BrewSessionFormDialog } from './BrewSessionFormDialog';
import { DeleteBrewSessionDialog } from './DeleteBrewSessionDialog';

function orDash(value: string | null): string {
  return value && value.trim() !== '' ? value : '—';
}

function NoteSection({ title, text }: { title: string; text: string | null }) {
  if (!text || text.trim() === '') {
    return null;
  }
  return (
    <Box sx={{ mb: 3 }}>
      <Typography variant="subtitle1" component="h2" gutterBottom>
        {title}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
        {text}
      </Typography>
    </Box>
  );
}

/** One brew session with every field, plus edit and delete. */
export function BrewSessionDetailView({ sessionId }: { sessionId: number }) {
  const t = useTranslations('brewSessions.detail');
  const tf = useTranslations('brewSessions.fields');
  const tc = useTranslations('common');
  const locale = useLocale();
  const router = useRouter();
  const { data: session, isLoading, isError, refetch } = useBrewSession(sessionId);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  if (isLoading && !session) {
    return <Spinner />;
  }

  if (isError || !session) {
    return <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  }

  const stats: Array<{ label: string; value: string }> = [
    { label: tf('rating'), value: session.rating === null ? t('notRated') : `${session.rating}/10` },
    { label: tf('actualGrind'), value: orDash(session.actualGrind) },
    {
      label: tf('actualTemp'),
      value: session.actualTemp === null ? '—' : tc('celsius', { value: session.actualTemp }),
    },
    { label: tf('actualTime'), value: orDash(session.actualTime) },
  ];

  return (
    <>
      <Button component={NextLink} href="/brew-sessions" size="small" sx={{ mb: 1 }}>
        {t('back')}
      </Button>

      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 2,
          mb: 3,
        }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="h5" component="h1" gutterBottom>
            <Link component={NextLink} href={`/recipes/${session.recipeId}`} underline="hover" color="inherit">
              {session.recipeName}
            </Link>
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('brewedOn', { date: formatDateTime(session.brewedAt, undefined, locale) })}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<EditOutlinedIcon />} onClick={() => setEditing(true)}>
            {t('edit')}
          </Button>
          <Button
            variant="outlined"
            color="error"
            startIcon={<DeleteOutlinedIcon />}
            onClick={() => setDeleting(true)}
          >
            {t('delete')}
          </Button>
        </Stack>
      </Box>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {stats.map((item) => (
          <Grid key={item.label} size={{ xs: 6, md: 3 }}>
            <StatCard label={item.label} value={item.value} />
          </Grid>
        ))}
      </Grid>

      <NoteSection title={tf('tasteResult')} text={session.tasteResult} />
      <NoteSection title={tf('adjustmentNotes')} text={session.adjustmentNotes} />

      {editing ? (
        <BrewSessionFormDialog open session={session} onClose={() => setEditing(false)} />
      ) : null}
      {deleting ? (
        <DeleteBrewSessionDialog
          open
          session={session}
          onClose={() => setDeleting(false)}
          onDeleted={() => router.push('/brew-sessions')}
        />
      ) : null}
    </>
  );
}
