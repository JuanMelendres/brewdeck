package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BrewRatiosTest {

  private static String ratio(String coffee, String water) {
    return BrewRatios.fromGrams(
        coffee == null ? null : new BigDecimal(coffee),
        water == null ? null : new BigDecimal(water));
  }

  @Test
  void dividesWaterByCoffee_withOneDecimalHalfUp() {
    assertThat(ratio("15", "250")).isEqualTo("1:16.7");
    assertThat(ratio("20", "300")).isEqualTo("1:15");
    assertThat(ratio("18", "36")).isEqualTo("1:2");
    assertThat(ratio("100", "800")).isEqualTo("1:8");
    // 21 / 20 = 1.05 exactly: half up gives 1.1.
    assertThat(ratio("20", "21")).isEqualTo("1:1.1");
  }

  @Test
  void largeRatiosArePlainNotScientific() {
    assertThat(ratio("10", "200")).isEqualTo("1:20");
    assertThat(ratio("0.01", "9999.99")).isEqualTo("1:999999");
  }

  @Test
  void isNullWithoutBothPositiveAmounts() {
    assertThat(ratio(null, "250")).isNull();
    assertThat(ratio("15", null)).isNull();
    assertThat(ratio("0", "250")).isNull();
  }
}
