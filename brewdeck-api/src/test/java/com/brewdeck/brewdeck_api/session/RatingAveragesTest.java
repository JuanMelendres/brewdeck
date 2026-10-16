package com.brewdeck.brewdeck_api.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RatingAveragesTest {

  @Test
  void roundsHalfUpToOneDecimal() {
    // 167 / 20: a binary toFixed(1) would show 8.3.
    assertThat(RatingAverages.round(167 / 20.0)).isEqualTo(8.4);
    assertThat(RatingAverages.round(25 / 3.0)).isEqualTo(8.3);
    assertThat(RatingAverages.round(26 / 3.0)).isEqualTo(8.7);
    assertThat(RatingAverages.round(9.0)).isEqualTo(9.0);
  }

  @Test
  void keepsNullWhenThereIsNothingToAverage() {
    assertThat(RatingAverages.round(null)).isNull();
  }
}
