package com.brewdeck.brewdeck_api.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class BrewdeckMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final BrewdeckMetrics metrics = new BrewdeckMetrics(registry);

  private double aiCalls(String outcome) {
    return registry
        .timer(
            BrewdeckMetrics.AI_REQUESTS,
            "operation",
            "improve",
            "provider",
            "ollama",
            "outcome",
            outcome)
        .count();
  }

  @Test
  void timeAi_returnsTheResult_andRecordsASuccess() {
    String result = metrics.timeAi("improve", "ollama", UnavailableForTest.class, () -> "recipe");

    assertThat(result).isEqualTo("recipe");
    assertThat(aiCalls("success")).isEqualTo(1);
  }

  @Test
  void timeAi_recordsUnavailable_andRethrows() {
    assertThatThrownBy(
            () ->
                metrics.timeAi(
                    "improve",
                    "ollama",
                    UnavailableForTest.class,
                    () -> {
                      throw new UnavailableForTest();
                    }))
        .isInstanceOf(UnavailableForTest.class);

    assertThat(aiCalls("unavailable")).isEqualTo(1);
    assertThat(aiCalls("success")).isZero();
  }

  @Test
  void timeAi_recordsOtherFailuresAsError() {
    assertThatThrownBy(
            () ->
                metrics.timeAi(
                    "improve",
                    "ollama",
                    UnavailableForTest.class,
                    () -> {
                      throw new IllegalStateException("bad JSON");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(aiCalls("error")).isEqualTo(1);
  }

  @Test
  void retentionRun_addsEachTotal() {
    metrics.retentionRun(3, 1, 2, 40);
    metrics.retentionRun(1, 0, 0, 5);

    assertThat(registry.counter(BrewdeckMetrics.RETENTION_ACCOUNTS, "result", "reminded").count())
        .isEqualTo(4);
    assertThat(registry.counter(BrewdeckMetrics.RETENTION_ACCOUNTS, "result", "deleted").count())
        .isEqualTo(1);
    assertThat(registry.counter(BrewdeckMetrics.RETENTION_ACCOUNTS, "result", "failed").count())
        .isEqualTo(2);
    assertThat(registry.counter(BrewdeckMetrics.RETENTION_TOKENS_DELETED).count()).isEqualTo(45);
  }

  private static final class UnavailableForTest extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
