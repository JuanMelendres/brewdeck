// Regenerates the app icons from the BrandMark geometry and the theme palette.
// Run from brewdeck-web/: `node scripts/generate-brand-icons.mjs` (Node 24+, which loads the .ts imports).
// Outputs are committed; rerun only when the mark or the palette changes.
import { writeFileSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { BACK_CARD_OPACITY, BRAND_MARK } from '../src/lib/brand/brandMark.ts';
import { palettes } from '../src/lib/theme/tokens.ts';

// sharp ships with Next as an optional dependency; resolve it from there instead of adding a direct one.
const require = createRequire(import.meta.url);
const sharp = createRequire(require.resolve('next/package.json'))('sharp');

const light = { tile: palettes.light.primary.main, mark: palettes.light.primary.contrastText };
const dark = { tile: palettes.dark.primary.main, mark: palettes.dark.primary.contrastText };

/** The mark's shapes on its 24 grid, colored by `currentColor`. */
function markShapes(variant) {
  const { backCard, frontCard, bean, strokeWidth } = BRAND_MARK[variant];
  const rect = (r, extra) =>
    `<rect x="${r.x}" y="${r.y}" width="${r.width}" height="${r.height}" rx="${r.rx}"${r.transform ? ` transform="${r.transform}"` : ''} ${extra}/>`;
  return `<mask id="bean-${variant}"><rect width="24" height="24" fill="white"/><g fill="none" stroke="black" stroke-width="${strokeWidth}" stroke-linecap="round"><ellipse cx="${bean.cx}" cy="${bean.cy}" rx="${bean.rx}" ry="${bean.ry}" transform="${bean.transform}"/><path d="${bean.seam}"/></g></mask>${
    backCard
      ? rect(backCard, `fill="none" stroke="currentColor" stroke-width="${strokeWidth}" opacity="${BACK_CARD_OPACITY}"`)
      : ''
  }${rect(frontCard, `fill="currentColor" mask="url(#bean-${variant})"`)}`;
}

/**
 * A square icon: a tile (rounded, or full-bleed when `cornerRatio` is 0) with the mark centered at
 * `markRatio` of its width. `darkScheme` adds a prefers-color-scheme switch (SVG favicons only).
 */
function iconSvg({ size, variant = 'full', markRatio = 0.62, cornerRatio = 0.28, darkScheme = false }) {
  const markSize = size * markRatio;
  const offset = (size - markSize) / 2;
  const style = darkScheme
    ? `<style>.tile{fill:${light.tile}}.mark{color:${light.mark}}@media (prefers-color-scheme: dark){.tile{fill:${dark.tile}}.mark{color:${dark.mark}}}</style>`
    : '';
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">${style}<rect class="tile" width="${size}" height="${size}" rx="${size * cornerRatio}" fill="${light.tile}"/><g class="mark" color="${light.mark}" transform="translate(${offset} ${offset}) scale(${markSize / 24})">${markShapes(variant)}</g></svg>`;
}

const png = (svg) => sharp(Buffer.from(svg)).png().toBuffer();

/** An ICO file holding PNG images (supported by every current browser). */
function ico(images) {
  const header = Buffer.alloc(6 + 16 * images.length);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(images.length, 4);
  let offset = header.length;
  images.forEach(({ size, data }, index) => {
    const entry = 6 + 16 * index;
    header.writeUInt8(size >= 256 ? 0 : size, entry);
    header.writeUInt8(size >= 256 ? 0 : size, entry + 1);
    header.writeUInt16LE(1, entry + 4);
    header.writeUInt16LE(32, entry + 6);
    header.writeUInt32LE(data.length, entry + 8);
    header.writeUInt32LE(offset, entry + 12);
    offset += data.length;
  });
  return Buffer.concat([header, ...images.map((image) => image.data)]);
}

mkdirSync('public/icons', { recursive: true });

// Browser tab: SVG for current browsers (follows the OS color scheme), ICO for the rest.
writeFileSync('src/app/icon.svg', iconSvg({ size: 32, darkScheme: true }));
const favicons = await Promise.all(
  [16, 32, 48].map(async (size) => ({
    size,
    data: await png(iconSvg({ size, variant: size < 32 ? 'simple' : 'full', markRatio: size < 32 ? 0.82 : 0.7 })),
  })),
);
writeFileSync('src/app/favicon.ico', ico(favicons));

// iOS home screen: full-bleed square, iOS rounds the corners itself.
writeFileSync('src/app/apple-icon.png', await png(iconSvg({ size: 180, cornerRatio: 0 })));

// Web app manifest: rounded icons, plus a maskable one with the mark inside the central 80 % safe zone.
writeFileSync('public/icons/icon-192.png', await png(iconSvg({ size: 192 })));
writeFileSync('public/icons/icon-512.png', await png(iconSvg({ size: 512 })));
writeFileSync('public/icons/icon-maskable-512.png', await png(iconSvg({ size: 512, markRatio: 0.5, cornerRatio: 0 })));

console.log('Brand icons written.');
