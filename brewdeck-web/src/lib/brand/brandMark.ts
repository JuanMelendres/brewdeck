/**
 * Geometry of the BrewDeck mark (concept B, "bean on a deck"), on a 24 × 24 grid.
 * Shared by the `BrandMark` component and `scripts/generate-brand-icons.mjs`, so it imports nothing.
 * Decision: docs/product/spikes/brand-mark-spike.md.
 */

export type BrandMarkVariant = 'full' | 'simple';

type Rect = { x: number; y: number; width: number; height: number; rx: number; transform?: string };

export type BrandMarkGeometry = {
  /** Fanned card behind the front card, drawn as a faded outline. */
  backCard?: Rect;
  /** Solid card in the mark color. */
  frontCard: Rect;
  /** Bean outline and seam, cut out of the front card so the background shows through. */
  bean: { cx: number; cy: number; rx: number; ry: number; transform: string; seam: string };
  /** Stroke width of the back card and of the bean cut-out. */
  strokeWidth: number;
};

export const BRAND_MARK: Record<BrandMarkVariant, BrandMarkGeometry> = {
  full: {
    backCard: { x: 3, y: 2.8, width: 12, height: 15, rx: 2.5, transform: 'rotate(-9 9 10)' },
    frontCard: { x: 7.5, y: 5.5, width: 13, height: 16, rx: 2.5 },
    bean: {
      cx: 14,
      cy: 13.5,
      rx: 3.4,
      ry: 4.9,
      transform: 'rotate(28 14 13.5)',
      seam: 'M12.4 9.6c2.4 1.6 0.8 6.2 3.2 7.8',
    },
    strokeWidth: 1.8,
  },
  // Below 32 px the back card blurs away, so the small variant keeps only a larger front card and bean.
  simple: {
    frontCard: { x: 5, y: 3, width: 14, height: 18, rx: 2.8 },
    bean: {
      cx: 12,
      cy: 12,
      rx: 3.6,
      ry: 5.4,
      transform: 'rotate(28 12 12)',
      seam: 'M10.4 7.7c2.6 1.8 0.6 6.8 3.2 8.6',
    },
    strokeWidth: 2.2,
  },
};

/** Opacity of the back card outline. */
export const BACK_CARD_OPACITY = 0.5;
