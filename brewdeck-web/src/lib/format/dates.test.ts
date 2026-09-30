import { describe, expect, it } from 'vitest';
import { compareInstants, formatDate, formatDateTime, localDayKey } from './dates';

describe('date formatting', () => {
  // 03:30 UTC on Sep 30 is still Sep 29 (21:30) in Mexico City (UTC-6).
  const lateEveningInMexico = '2026-09-30T03:30:00Z';

  it('localDayKey uses the viewer zone, not the UTC date in the string', () => {
    expect(localDayKey(lateEveningInMexico, 'UTC')).toBe('2026-09-30');
    expect(localDayKey(lateEveningInMexico, 'America/Mexico_City')).toBe('2026-09-29');
  });

  it('formatDateTime renders the instant in the viewer zone', () => {
    expect(formatDateTime(lateEveningInMexico, 'America/Mexico_City')).toMatch(/9:30/);
    expect(formatDateTime(lateEveningInMexico, 'UTC')).toMatch(/3:30/);
  });

  it('formatDate shows the local calendar day', () => {
    expect(formatDate(lateEveningInMexico, 'America/Mexico_City')).toMatch(/29/);
  });

  it('returns a dash for missing or invalid values', () => {
    expect(formatDateTime(null)).toBe('—');
    expect(formatDate(undefined)).toBe('—');
    expect(formatDate('not-a-date')).toBe('—');
  });

  it('compareInstants orders by time even when fractional seconds differ in length', () => {
    // Lexically "…:25Z" > "…:25.5Z" ('Z' sorts after '.'), which would be wrong.
    expect('2026-09-30T10:00:25Z' > '2026-09-30T10:00:25.5Z').toBe(true);
    expect(compareInstants('2026-09-30T10:00:25Z', '2026-09-30T10:00:25.5Z')).toBeLessThan(0);
  });
});
