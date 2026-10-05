'use client';

import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useTranslations } from 'next-intl';
import type { BrewMethod } from '@/lib/api/brewMethods';
import { radius } from '@/lib/theme/tokens';

export function BrewMethodsTable({
  methods,
  onEdit,
  onDelete,
}: {
  methods: BrewMethod[];
  onEdit?: (method: BrewMethod) => void;
  onDelete?: (method: BrewMethod) => void;
}) {
  const t = useTranslations('brewMethods.list');
  const tc = useTranslations('common');
  return (
    <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: radius.card }}>
      <Table sx={{ minWidth: 560, '& td, & th': { px: 2.5 }, '& td': { py: 1.75 } }}>
        <TableHead>
          <TableRow>
            <TableCell>{t('method')}</TableCell>
            <TableCell>{t('type')}</TableCell>
            <TableCell align="right">{tc('actions')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {methods.map((method) => (
            <TableRow key={method.id} hover>
              <TableCell>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {method.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {method.description && method.description.trim() !== '' ? method.description : '—'}
                </Typography>
              </TableCell>
              <TableCell>
                {method.shared ? (
                  <Chip label={t('shared')} size="small" variant="outlined" />
                ) : (
                  <Chip label={t('mine')} size="small" sx={{ bgcolor: 'background.tint', color: 'secondary.main' }} />
                )}
              </TableCell>
              <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                {/* Shared-catalog methods are read-only for regular users; the backend enforces it. */}
                {method.shared ? null : (
                  <>
                    <IconButton
                      aria-label={tc('editItem', { name: method.name })}
                      size="small"
                      onClick={() => onEdit?.(method)}
                    >
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      aria-label={tc('deleteItem', { name: method.name })}
                      size="small"
                      onClick={() => onDelete?.(method)}
                    >
                      <DeleteOutlinedIcon fontSize="small" />
                    </IconButton>
                  </>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
