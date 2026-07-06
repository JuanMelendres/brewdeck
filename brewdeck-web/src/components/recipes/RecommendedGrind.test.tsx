import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecommendedGrind } from './RecommendedGrind';
import * as hook from '@/hooks/useRecipeBrewSessions';
import type { BrewSession, PageResponse } from '@/lib/api/types';

type HookReturn = ReturnType<typeof hook.useRecipeBrewSessions>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useRecipeBrewSessions').mockReturnValue(value as HookReturn);
}

function session(
  id: number,
  brewedAt: string,
  rating: number | null,
  actualGrind: string | null,
): BrewSession {
  return {
    id,
    recipeId: 1,
    recipeName: 'Recipe',
    brewedAt,
    actualGrind,
    actualTemp: null,
    actualTime: null,
    tasteResult: null,
    rating,
    adjustmentNotes: null,
  };
}

function page(content: BrewSession[]): PageResponse<BrewSession> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1, first: true, last: true };
}

afterEach(() => vi.restoreAllMocks());

describe('RecommendedGrind', () => {
  it('renders nothing while loading', () => {
    mockHook({ isLoading: true, data: undefined });
    const { container } = renderWithTheme(<RecommendedGrind recipeId={1} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('prompts to brew when there are no rated sessions with a grind', () => {
    mockHook({
      isLoading: false,
      data: page([session(1, '2026-07-01T10:00:00', null, 'Timemore 18'), session(2, '2026-07-02T10:00:00', 8, '  ')]),
    });
    renderWithTheme(<RecommendedGrind recipeId={1} />);
    expect(screen.getByText(/brew and rate a session to get a grind recommendation/i)).toBeInTheDocument();
  });

  it('recommends the grind of the highest-rated session', () => {
    mockHook({
      isLoading: false,
      data: page([
        session(1, '2026-07-01T10:00:00', 6, 'Timemore 20'),
        session(2, '2026-07-02T10:00:00', 9, 'Timemore 18'),
        session(3, '2026-07-03T10:00:00', 7, 'Timemore 22'),
      ]),
    });
    renderWithTheme(<RecommendedGrind recipeId={1} />);
    expect(screen.getByText('Timemore 18')).toBeInTheDocument();
    expect(screen.getByText(/best-rated session \(9\/10\)/i)).toBeInTheDocument();
  });

  it('breaks rating ties by the most recent session', () => {
    mockHook({
      isLoading: false,
      data: page([
        session(1, '2026-07-01T10:00:00', 9, 'Old Grind'),
        session(2, '2026-07-05T10:00:00', 9, 'New Grind'),
      ]),
    });
    renderWithTheme(<RecommendedGrind recipeId={1} />);
    expect(screen.getByText('New Grind')).toBeInTheDocument();
    expect(screen.queryByText('Old Grind')).not.toBeInTheDocument();
  });
});
