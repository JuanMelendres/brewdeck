import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsTable } from './BrewSessionsTable';
import type { BrewSession } from '@/lib/api/types';
import { formatDateTime } from '@/lib/format/dates';

const base: BrewSession = {
  id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00Z',
  actualGrind: 'S3 5.5', actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
  rating: 9, adjustmentNotes: null,
};

const empty: BrewSession = {
  ...base, id: 2, recipeName: 'Plain V60', brewedAt: '2026-02-02T08:15:00Z',
  actualTemp: null, actualTime: null, tasteResult: null, rating: null,
};

describe('BrewSessionsTable', () => {
  it('renders session rows with the recipe link, formatted date, rating, and units', () => {
    renderWithTheme(<BrewSessionsTable sessions={[base, empty]} />);

    expect(screen.getByRole('link', { name: 'Mezcla AeroPress' })).toHaveAttribute('href', '/recipes/1');
    // Rendered in the viewer's zone, never the raw UTC string.
    expect(screen.getByText(formatDateTime(base.brewedAt))).toBeInTheDocument();
    expect(screen.queryByText(/2026-01-01T10:30/)).not.toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('/10')).toBeInTheDocument();
    expect(screen.getByText('90°C')).toBeInTheDocument();
    expect(screen.getByText('2:30')).toBeInTheDocument();
    expect(screen.getByText('Clean')).toBeInTheDocument();
  });

  it('shows a dash for each missing value', () => {
    renderWithTheme(<BrewSessionsTable sessions={[empty]} />);

    expect(screen.getByText(formatDateTime(empty.brewedAt))).toBeInTheDocument();
    // Null rating, actualTemp, actualTime, and tasteResult.
    expect(screen.getAllByText('—')).toHaveLength(4);
  });

  it('shows short taste notes in full, without a toggle', () => {
    renderWithTheme(<BrewSessionsTable sessions={[base]} />);

    expect(screen.getByText('Clean')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Show more' })).not.toBeInTheDocument();
  });

  it('lets the user expand and collapse long taste notes', async () => {
    const user = userEvent.setup();
    const longTaste =
      'Bright citrus up front, then a syrupy body with brown sugar and a long cocoa finish. ' +
      'Slightly sour when it cooled; grind one step finer next time.';
    renderWithTheme(<BrewSessionsTable sessions={[{ ...base, tasteResult: longTaste }]} />);

    // The whole text is in the document (clamped visually), so screen readers get all of it.
    expect(screen.getByText(longTaste)).toBeInTheDocument();
    const toggle = screen.getByRole('button', { name: 'Show more' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    await user.click(toggle);
    expect(screen.getByRole('button', { name: 'Show less' })).toHaveAttribute('aria-expanded', 'true');

    await user.click(screen.getByRole('button', { name: 'Show less' }));
    expect(screen.getByRole('button', { name: 'Show more' })).toBeInTheDocument();
  });

  it('links each brew date to the session detail page', () => {
    renderWithTheme(<BrewSessionsTable sessions={[base]} />);

    expect(screen.getByRole('link', { name: /open the brew session from/i })).toHaveAttribute(
      'href',
      `/brew-sessions/${base.id}`,
    );
  });
});
