package com.brewdeck.brewdeck_api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class MaxUtf8BytesValidatorTest {

  private record Holder(@MaxUtf8Bytes(value = 72, message = "too long") String value) {}

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private boolean isValid(String value) {
    return validator.validate(new Holder(value)).isEmpty();
  }

  @Test
  void acceptsNull() {
    assertThat(isValid(null)).isTrue();
  }

  @Test
  void limitIsOnBytesNotCharacters() {
    assertThat(isValid("a".repeat(72))).isTrue();
    assertThat(isValid("a".repeat(73))).isFalse();
    // "é" is 2 bytes in UTF-8: 36 of them fit exactly, 37 (74 bytes) do not, even though 37
    // characters would pass a character-based @Size(max = 72).
    assertThat(isValid("é".repeat(36))).isTrue();
    assertThat(isValid("é".repeat(37))).isFalse();
    // Emoji are 4 bytes each.
    assertThat(isValid("☕".repeat(24))).isTrue();
    assertThat(isValid("😀".repeat(19))).isFalse();
  }
}
