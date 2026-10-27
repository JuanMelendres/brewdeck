package com.brewdeck.brewdeck_api.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OtlpExportCheckTest {

  @Test
  void passes_whenUrlAndAuthorizationAreSet() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty(OtlpExportCheck.URL, "https://otlp.example/otlp/v1/metrics")
            .withProperty(OtlpExportCheck.AUTHORIZATION, "Basic dGVzdA==");

    assertThatCode(() -> new OtlpExportCheck(environment)).doesNotThrowAnyException();
  }

  @Test
  void fails_whenTheUrlIsMissing() {
    MockEnvironment environment =
        new MockEnvironment().withProperty(OtlpExportCheck.AUTHORIZATION, "Basic dGVzdA==");

    assertThatThrownBy(() -> new OtlpExportCheck(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OTLP_METRICS_URL");
  }

  @Test
  void fails_whenTheAuthorizationIsBlank() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty(OtlpExportCheck.URL, "https://otlp.example/otlp/v1/metrics")
            .withProperty(OtlpExportCheck.AUTHORIZATION, " ");

    assertThatThrownBy(() -> new OtlpExportCheck(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OTLP_METRICS_AUTHORIZATION")
        // The secret's value never ends up in a log line.
        .hasMessageNotContaining("Basic");
  }
}
