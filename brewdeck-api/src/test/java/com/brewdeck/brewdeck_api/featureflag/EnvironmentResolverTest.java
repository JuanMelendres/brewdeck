package com.brewdeck.brewdeck_api.featureflag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EnvironmentResolverTest {

  @Test
  void resolvesKnownActiveProfile() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("test");

    assertThat(new EnvironmentResolver(env, "").resolve()).isEqualTo("test");
  }

  @Test
  void explicitOverrideWins_overActiveProfile() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("local");

    assertThat(new EnvironmentResolver(env, "staging").resolve()).isEqualTo("staging");
  }

  @Test
  void noActiveProfile_defaultsToProd() {
    MockEnvironment env = new MockEnvironment();

    assertThat(new EnvironmentResolver(env, "").resolve()).isEqualTo("prod");
  }

  @Test
  void unrecognizedProfile_defaultsToProd_failSafe() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("weird-profile");

    assertThat(new EnvironmentResolver(env, "").resolve()).isEqualTo("prod");
  }

  @Test
  void overrideIsNormalizedToLowercase() {
    MockEnvironment env = new MockEnvironment();

    assertThat(new EnvironmentResolver(env, "  DEV  ").resolve()).isEqualTo("dev");
  }
}
