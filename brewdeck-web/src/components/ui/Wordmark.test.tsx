import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Wordmark } from './Wordmark';

describe('Wordmark', () => {
  it('reads as one word with "Brew" bolder than "Deck"', () => {
    render(<Wordmark />);
    const brew = screen.getByText('Brew');
    const deck = screen.getByText('Deck');

    expect(brew.parentElement).toHaveTextContent(/^BrewDeck$/);
    expect(brew).toHaveStyle({ fontWeight: 700 });
    expect(deck).toHaveStyle({ fontWeight: 400 });
  });
});
