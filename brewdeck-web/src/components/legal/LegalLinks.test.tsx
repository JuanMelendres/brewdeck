import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LegalLinks } from './LegalLinks';
import { renderWithTheme } from '@/test/renderWithTheme';

let legalStatus = { isLoading: false, published: true, termsVersion: '2026-10-20' };
vi.mock('@/hooks/useLegalStatus', () => ({ useLegalStatus: () => legalStatus }));

describe('LegalLinks', () => {
  afterEach(() => {
    legalStatus = { isLoading: false, published: true, termsVersion: '2026-10-20' };
  });

  it('links the privacy notice and terms once published', () => {
    renderWithTheme(<LegalLinks />, { locale: 'es' });

    expect(screen.getByRole('link', { name: 'Aviso de privacidad' })).toHaveAttribute('href', '/privacy');
    expect(screen.getByRole('link', { name: 'Términos' })).toHaveAttribute('href', '/terms');
  });

  it('renders nothing before they are published', () => {
    legalStatus = { isLoading: false, published: false, termsVersion: '2026-10-20' };
    const { container } = renderWithTheme(<LegalLinks />);

    expect(container).toBeEmptyDOMElement();
  });
});
