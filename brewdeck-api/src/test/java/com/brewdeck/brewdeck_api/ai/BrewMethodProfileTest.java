package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrewMethodProfileTest {

  private static String grindFor(String method) {
    return BrewMethodProfile.forMethod(method).map(BrewMethodProfile::grind).orElse("none");
  }

  @Test
  void methodsAreRecognizedByName_mostSpecificFirst() {
    assertThat(grindFor("AeroPress")).startsWith("fine to medium");
    assertThat(grindFor("French Press")).startsWith("coarse");
    assertThat(grindFor("Chemex")).startsWith("medium-coarse");
    assertThat(grindFor("Hario V60 02")).startsWith("medium-fine");
    assertThat(grindFor("Cold Brew")).startsWith("coarse to extra-coarse");
    assertThat(grindFor("Siphon")).isEqualTo("none");
  }
}
