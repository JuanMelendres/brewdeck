'use client';

import { useId } from 'react';
import { BACK_CARD_OPACITY, BRAND_MARK, type BrandMarkVariant } from '@/lib/brand/brandMark';

/**
 * The BrewDeck mark, drawn in `currentColor`. Decorative: always pair it with the visible
 * "BrewDeck" name (see `Wordmark`). The bean is cut out of the card, so whatever sits
 * behind the mark shows through it.
 */
export function BrandMark({ size = 24, variant = 'full' }: { size?: number; variant?: BrandMarkVariant }) {
  const maskId = `brand-mark-${useId().replace(/:/g, '')}`;
  const { backCard, frontCard, bean, strokeWidth } = BRAND_MARK[variant];

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      data-variant={variant}
    >
      <defs>
        <mask id={maskId}>
          <rect width="24" height="24" fill="white" stroke="none" />
          <g stroke="black">
            <ellipse cx={bean.cx} cy={bean.cy} rx={bean.rx} ry={bean.ry} transform={bean.transform} />
            <path d={bean.seam} />
          </g>
        </mask>
      </defs>
      {backCard && <rect {...backCard} opacity={BACK_CARD_OPACITY} />}
      <rect {...frontCard} fill="currentColor" stroke="none" mask={`url(#${maskId})`} />
    </svg>
  );
}
