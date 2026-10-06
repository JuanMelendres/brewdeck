package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class UserLocaleTest {

  @AfterEach
  void clearRequestLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void savedLanguageWinsOverTheRequest() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
    User user = User.builder().language(Language.ES).build();

    assertThat(UserLocale.of(user)).isEqualTo(Locale.forLanguageTag("es"));
  }

  @Test
  void withoutASavedLanguage_usesTheRequestLanguage() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

    assertThat(UserLocale.of(User.builder().build())).isEqualTo(Locale.forLanguageTag("es"));
  }

  @Test
  void outsideARequest_usesEnglishNotTheJvmLocale() {
    assertThat(UserLocale.of(User.builder().build())).isEqualTo(Locale.ENGLISH);
  }
}
