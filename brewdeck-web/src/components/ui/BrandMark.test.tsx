import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BrandMark } from './BrandMark';

describe('BrandMark', () => {
  it('is decorative, so screen readers skip it', () => {
    const { container } = render(<BrandMark />);

    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('draws the fanned back card only in the full variant', () => {
    const full = render(<BrandMark variant="full" />).container;
    const simple = render(<BrandMark variant="simple" />).container;

    expect(full.querySelectorAll('svg > rect')).toHaveLength(2);
    expect(simple.querySelectorAll('svg > rect')).toHaveLength(1);
  });

  it('gives each instance its own bean cut-out', () => {
    const { container } = render(
      <>
        <BrandMark />
        <BrandMark />
      </>,
    );
    const ids = Array.from(container.querySelectorAll('mask')).map((mask) => mask.id);

    expect(new Set(ids).size).toBe(2);
    ids.forEach((id) => expect(container.querySelector(`[mask="url(#${id})"]`)).not.toBeNull());
  });

  it('scales to the requested size', () => {
    const { container } = render(<BrandMark size={40} />);

    expect(container.querySelector('svg')).toHaveAttribute('width', '40');
  });
});
