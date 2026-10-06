import Box from '@mui/material/Box';
import Typography, { type TypographyProps } from '@mui/material/Typography';

/** "BrewDeck" in the display font: "Brew" bold, "Deck" regular (brand-mark spike, wordmark option 1). */
export function Wordmark({ variant = 'h6' }: { variant?: TypographyProps['variant'] }) {
  return (
    <Typography variant={variant} component="span" noWrap>
      {/* Inline spans with no space between them, so the name still reads as one word. The brand
          name is never translated. */}
      <Box component="span" sx={{ fontWeight: 700 }}>
        {'Brew'}
      </Box>
      <Box component="span" sx={{ fontWeight: 400 }}>
        {'Deck'}
      </Box>
    </Typography>
  );
}
