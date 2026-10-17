package com.brewdeck.brewdeck_api.recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The brew ratio derived from the grams, as "1:&lt;water / coffee&gt;" with one decimal, half up
 * ("1:16.7", "1:15"). Owner decision on 2026-10-16 (DATA-01, option A): when both grams are set the
 * ratio is computed, so it can never contradict them; free text stays only for recipes without
 * grams.
 */
public final class BrewRatios {

  private BrewRatios() {}

  /** The derived ratio, or {@code null} when either amount is missing or not positive. */
  public static String fromGrams(BigDecimal coffeeGrams, BigDecimal waterGrams) {
    if (coffeeGrams == null
        || waterGrams == null
        || coffeeGrams.signum() <= 0
        || waterGrams.signum() <= 0) {
      return null;
    }
    BigDecimal water = waterGrams.divide(coffeeGrams, 1, RoundingMode.HALF_UP);
    return "1:" + water.stripTrailingZeros().toPlainString();
  }
}
