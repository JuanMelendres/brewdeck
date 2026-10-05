import { describe, expect, it } from 'vitest';
import en from '../../messages/en.json';
import es from '../../messages/es.json';

function keys(messages: object, prefix = ''): string[] {
  return Object.entries(messages).flatMap(([key, value]) =>
    typeof value === 'object' && value !== null ? keys(value, `${prefix}${key}.`) : [`${prefix}${key}`],
  );
}

describe('messages', () => {
  it('has the same keys in Spanish and English', () => {
    expect(keys(es).sort()).toEqual(keys(en).sort());
  });
});
