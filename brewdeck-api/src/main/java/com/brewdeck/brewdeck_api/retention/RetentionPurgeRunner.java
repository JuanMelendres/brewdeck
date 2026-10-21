package com.brewdeck.brewdeck_api.retention;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * One-shot mode for the Cloud Run Job (ADR-021): with {@code brewdeck.retention.run-once=true} the
 * app runs the purge once and {@code BrewdeckApiApplication} exits with this exit code, non-zero
 * when anything failed so the job execution shows as failed.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "brewdeck.retention", name = "run-once", havingValue = "true")
public class RetentionPurgeRunner implements ApplicationRunner, ExitCodeGenerator {

  private final RetentionPurgeService purgeService;
  private int exitCode;

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // any failure must become exit code 1
  public void run(ApplicationArguments args) {
    try {
      RetentionPurgeResult result = purgeService.run();
      exitCode = result.failures() == 0 ? 0 : 1;
    } catch (RuntimeException e) {
      log.error("Retention purge failed", e);
      exitCode = 1;
    }
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}
