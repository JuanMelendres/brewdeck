package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class EmailAddressesTest {

  @Test
  void trimsAndLowercases() {
    assertThat(EmailAddresses.normalize("  Juan.Mele@Example.COM "))
        .isEqualTo("juan.mele@example.com");
  }

  @Test
  void nullStaysNull() {
    assertThat(EmailAddresses.normalize(null)).isNull();
  }

  @Test
  void isIndependentOfTheDefaultLocale() {
    Locale original = Locale.getDefault();
    try {
      // In Turkish, "I".toLowerCase() is a dotless "ı" -- which would break lookups.
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      assertThat(EmailAddresses.normalize("INFO@EXAMPLE.COM")).isEqualTo("info@example.com");
    } finally {
      Locale.setDefault(original);
    }
  }

  @Test
  void requestRecordsNormalizeTheirEmail() {
    assertThat(new RegisterRequest(" A@B.COM ", "password1").email()).isEqualTo("a@b.com");
    assertThat(new LoginRequest("A@B.COM", "password1").email()).isEqualTo("a@b.com");
    assertThat(new com.brewdeck.brewdeck_api.auth.reset.ForgotPasswordRequest("A@B.COM").email())
        .isEqualTo("a@b.com");
  }
}
