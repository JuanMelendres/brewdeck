import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LegalDocument } from './LegalDocument';
import { renderWithTheme } from '@/test/renderWithTheme';

let legalStatus = { isLoading: false, published: true, termsVersion: '2026-10-20' };
vi.mock('@/hooks/useLegalStatus', () => ({ useLegalStatus: () => legalStatus }));

const MARKDOWN = `# Privacy notice

## 1. Who is responsible

See the [terms](/terms).

| Name | Purpose |
|---|---|
| \`brewdeck_refresh\` | Keep you signed in |
`;

describe('LegalDocument', () => {
  afterEach(() => {
    legalStatus = { isLoading: false, published: true, termsVersion: '2026-10-20' };
  });

  it('renders the Markdown, including tables and internal links, once published', () => {
    renderWithTheme(<LegalDocument markdown={MARKDOWN} />);

    expect(screen.getByRole('heading', { level: 1, name: 'Privacy notice' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: '1. Who is responsible' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'terms' })).toHaveAttribute('href', '/terms');
    expect(screen.getByRole('cell', { name: 'Keep you signed in' })).toBeInTheDocument();
  });

  it('hides the draft text until the API reports it as published', () => {
    legalStatus = { isLoading: false, published: false, termsVersion: '2026-10-20' };
    renderWithTheme(<LegalDocument markdown={MARKDOWN} />);

    expect(screen.getByText('This page is not available yet.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Privacy notice' })).not.toBeInTheDocument();
  });
});
