/**
 * The API returns timestamps as ISO-8601 UTC instants ("2026-09-30T17:27:25Z"). Never slice those
 * strings for display, because that would show UTC wall-clock time. Format them in the viewer's
 * zone. `timeZone` is only passed by tests; in the app it defaults to the browser's zone.
 */

/**
 * The UI is English-only until i18n lands (UI/UX refresh workstream 8), so dates use English too;
 * the browser locale would mix languages ("30 sept 2026" next to English labels).
 */
export const UI_LOCALE = 'en-US';

const DASH = '—';

function parse(iso: string | null | undefined): Date | null {
  if (!iso) {
    return null;
  }
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Date and time in the viewer's zone, e.g. "Sep 30, 2026, 11:27 AM". */
export function formatDateTime(iso: string | null | undefined, timeZone?: string): string {
  const date = parse(iso);
  if (!date) {
    return DASH;
  }
  return new Intl.DateTimeFormat(UI_LOCALE, {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone,
  }).format(date);
}

/** Date only, in the viewer's zone, e.g. "Sep 30, 2026". */
export function formatDate(iso: string | null | undefined, timeZone?: string): string {
  const date = parse(iso);
  if (!date) {
    return DASH;
  }
  return new Intl.DateTimeFormat(UI_LOCALE, { dateStyle: 'medium', timeZone }).format(date);
}

/** Calendar day ("YYYY-MM-DD") of an instant in the viewer's zone, e.g. for chart labels. */
export function localDayKey(iso: string, timeZone?: string): string {
  const date = parse(iso);
  if (!date) {
    return DASH;
  }
  // en-CA formats dates as YYYY-MM-DD.
  return new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone,
  }).format(date);
}

/**
 * Chronological comparator for ISO instants. Don't compare the strings themselves: fractional
 * seconds vary in length ("…25Z" vs "…25.5Z"), so lexical order is not time order.
 */
export function compareInstants(a: string, b: string): number {
  return Date.parse(a) - Date.parse(b);
}
