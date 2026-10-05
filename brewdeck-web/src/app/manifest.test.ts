import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { palettes } from '@/lib/theme/tokens';
import manifest from './manifest';

const PUBLIC_DIR = join(__dirname, '..', '..', 'public');

describe('manifest', () => {
  const result = manifest();

  it('opens BrewDeck as a standalone app from the home screen', () => {
    expect(result).toMatchObject({ name: 'BrewDeck', start_url: '/', display: 'standalone' });
    expect(result.theme_color).toBe(palettes.light.primary.main);
  });

  it('lists 192 and 512 px icons plus a maskable one, all present in public/', () => {
    const icons = result.icons ?? [];

    expect(icons.map((icon) => `${icon.sizes} ${icon.purpose}`)).toEqual([
      '192x192 any',
      '512x512 any',
      '512x512 maskable',
    ]);
    icons.forEach((icon) => expect(existsSync(join(PUBLIC_DIR, icon.src))).toBe(true));
  });
});
