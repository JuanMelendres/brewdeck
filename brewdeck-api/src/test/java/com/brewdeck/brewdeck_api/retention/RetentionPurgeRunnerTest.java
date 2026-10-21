package com.brewdeck.brewdeck_api.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class RetentionPurgeRunnerTest {

  @Mock private RetentionPurgeService purgeService;
  @InjectMocks private RetentionPurgeRunner runner;

  @Test
  void cleanRun_exitsZero() {
    when(purgeService.run()).thenReturn(new RetentionPurgeResult(true, 2, 1, 0, 40));

    runner.run(new DefaultApplicationArguments());

    assertThat(runner.getExitCode()).isZero();
  }

  @Test
  void accountFailures_exitNonZero() {
    when(purgeService.run()).thenReturn(new RetentionPurgeResult(true, 2, 1, 1, 40));

    runner.run(new DefaultApplicationArguments());

    assertThat(runner.getExitCode()).isEqualTo(1);
  }

  @Test
  void crash_exitsNonZero() {
    when(purgeService.run()).thenThrow(new IllegalStateException("database down"));

    runner.run(new DefaultApplicationArguments());

    assertThat(runner.getExitCode()).isEqualTo(1);
  }
}
