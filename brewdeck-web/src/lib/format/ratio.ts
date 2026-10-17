/**
 * The brew ratio the API derives from the grams: "1:<water / coffee>" with one decimal, half up
 * ("1:16.7", "1:15"). Mirrors `BrewRatios.fromGrams` (DATA-01, option A) so the form can show the
 * value before saving; the API computes it again on save.
 *
 * Grams have at most two decimals, so both are scaled to whole centigrams and the division and
 * rounding are done in integers, without binary float drift.
 */
export function deriveRatio(coffeeGrams: unknown, waterGrams: unknown): string | null {
  const coffee = toCentigrams(coffeeGrams);
  const water = toCentigrams(waterGrams);
  if (coffee === null || water === null) {
    return null;
  }
  // tenths = round_half_up(10 * water / coffee) = floor((20 * water + coffee) / (2 * coffee))
  const tenths = Math.floor((20 * water + coffee) / (2 * coffee));
  const whole = Math.floor(tenths / 10);
  const decimal = tenths % 10;
  return decimal === 0 ? `1:${whole}` : `1:${whole}.${decimal}`;
}

function toCentigrams(value: unknown): number | null {
  if (value === '' || value === null || value === undefined) {
    return null;
  }
  const grams = Number(value);
  if (!Number.isFinite(grams) || grams <= 0) {
    return null;
  }
  return Math.round(grams * 100);
}
