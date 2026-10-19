package com.brewdeck.brewdeck_api.session;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Average ratings as the API returns them: one decimal, half up (DATA-01). SQL keeps ordering by
 * the full value; only the number in the response is rounded, so 167 / 20 = 8.35 reads 8.4 instead
 * of the 8.3 a binary {@code toFixed(1)} would show.
 */
public final class RatingAverages {

  private RatingAverages() {}

  /** The average rounded to one decimal, or {@code null} when there was nothing to average. */
  public static Double round(Double average) {
    if (average == null) {
      return null;
    }
    // valueOf(double) goes through Double.toString, so 8.35 is "8.35", not 8.3499999...
    return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
