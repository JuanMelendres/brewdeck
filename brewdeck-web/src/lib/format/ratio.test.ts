import { describe, expect, it } from 'vitest';
import { deriveRatio } from './ratio';

describe('deriveRatio', () => {
  it('matches the API: water / coffee, one decimal, half up', () => {
    expect(deriveRatio(15, 250)).toBe('1:16.7');
    expect(deriveRatio('20', '300')).toBe('1:15');
    expect(deriveRatio(18, 36)).toBe('1:2');
    expect(deriveRatio(100, 800)).toBe('1:8');
    // 21 / 20 = 1.05 exactly: half up gives 1.1, which a float toFixed would not guarantee.
    expect(deriveRatio(20, 21)).toBe('1:1.1');
    expect(deriveRatio(0.01, 9999.99)).toBe('1:999999');
  });

  it('is null until both amounts are positive numbers', () => {
    expect(deriveRatio('', 250)).toBeNull();
    expect(deriveRatio(15, undefined)).toBeNull();
    expect(deriveRatio(0, 250)).toBeNull();
    expect(deriveRatio('abc', 250)).toBeNull();
  });
});
