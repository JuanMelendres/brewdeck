package com.brewdeck.brewdeck_api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class StartupSafetyCheckTest {

  private static final String REAL_SECRET = "a-real-secret-from-secret-manager-0123456789";

  private static MockEnvironment withProfiles(String... profiles) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profiles);
    return environment;
  }

  @Test
  void devSecretConstant_matchesTheApplicationYamlFallback() throws IOException {
    try (InputStream yaml = new ClassPathResource("application.yaml").getInputStream()) {
      String content = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);

      assertThat(content)
          .contains("${BREWDECK_JWT_SECRET:" + StartupSafetyCheck.DEV_JWT_SECRET + "}");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"local", "test"})
  void devSecret_isAcceptedOnDeveloperProfiles(String profile) {
    assertThatCode(
            () ->
                new StartupSafetyCheck(
                    withProfiles(profile), StartupSafetyCheck.DEV_JWT_SECRET, false))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"dev", "staging", "prod", "weird-profile"})
  void devSecret_failsStartupOnAnyOtherProfile(String profile) {
    assertThatThrownBy(
            () ->
                new StartupSafetyCheck(
                    withProfiles(profile), StartupSafetyCheck.DEV_JWT_SECRET, true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BREWDECK_JWT_SECRET");
  }

  @Test
  void devSecret_failsStartupWithNoActiveProfile() {
    assertThatThrownBy(
            () -> new StartupSafetyCheck(withProfiles(), StartupSafetyCheck.DEV_JWT_SECRET, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BREWDECK_JWT_SECRET");
  }

  @ParameterizedTest
  @ValueSource(strings = {"staging", "prod"})
  void mailOff_failsStartupOnHostedProfiles(String profile) {
    assertThatThrownBy(() -> new StartupSafetyCheck(withProfiles(profile), REAL_SECRET, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BREWDECK_MAIL_ENABLED");
  }

  @ParameterizedTest
  @ValueSource(strings = {"staging", "prod"})
  void hostedProfile_startsWithRealSecretAndMailOn(String profile) {
    assertThatCode(() -> new StartupSafetyCheck(withProfiles(profile), REAL_SECRET, true))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"local", "dev", "test"})
  void mailOff_isAllowedOutsideHostedProfiles(String profile) {
    assertThatCode(() -> new StartupSafetyCheck(withProfiles(profile), REAL_SECRET, false))
        .doesNotThrowAnyException();
  }
}
