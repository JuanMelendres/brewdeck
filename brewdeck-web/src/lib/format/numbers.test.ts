import { describe, expect, it } from 'vitest';
import { formatAverage } from './numbers';

describe('formatAverage', () => {
  it('uses the decimal separator of the app language', () => {
    expect(formatAverage(8.4, 'en')).toBe('8.4');
    expect(formatAverage(8.4, 'es')).toBe('8,4');
  });

  it('always shows one decimal', () => {
    expect(formatAverage(9, 'en')).toBe('9.0');
    expect(formatAverage(9, 'es')).toBe('9,0');
  });

  it('shows a dash when there is nothing to average', () => {
    expect(formatAverage(null, 'en')).toBe('—');
  });
});
