import type { MetadataRoute } from 'next';
import { palettes } from '@/lib/theme/tokens';

/** Lets phones add BrewDeck to the home screen. Icons come from `scripts/generate-brand-icons.mjs`. */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: 'BrewDeck',
    short_name: 'BrewDeck',
    description: 'Coffee brewing companion',
    start_url: '/',
    display: 'standalone',
    background_color: palettes.light.background.default,
    theme_color: palettes.light.primary.main,
    icons: [
      { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
      { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
      { src: '/icons/icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
    ],
  };
}
