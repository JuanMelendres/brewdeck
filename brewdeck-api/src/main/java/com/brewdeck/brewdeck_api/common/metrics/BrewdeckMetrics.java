package com.brewdeck.brewdeck_api.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * BrewDeck's own metrics (OBS-01 PR 2, ADR-023), in one place so names and tags stay consistent.
 *
 * <p>Tags only take values from small fixed sets (outcome, rule, email kind, AI operation). Never
 * tag with a user id, email or token: each distinct value is a new time series, and those values
 * are personal data.
 */
@Component
public class BrewdeckMetrics {

  static final String LOGINS = "brewdeck.auth.logins";
  static final String REFRESH_REUSE = "brewdeck.auth.refresh.reuse";
  static final String RATE_LIMIT_REJECTIONS = "brewdeck.rate.limit.rejections";
  static final String MAIL_SENT = "brewdeck.mail.sent";
  static final String AI_REQUESTS = "brewdeck.ai.requests";
  static final String RETENTION_ACCOUNTS = "brewdeck.retention.accounts";
  static final String RETENTION_TOKENS_DELETED = "brewdeck.retention.tokens.deleted";

  private static final String OUTCOME = "outcome";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";

  private final MeterRegistry registry;

  public BrewdeckMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void loginSucceeded() {
    registry.counter(LOGINS, OUTCOME, SUCCESS).increment();
  }

  /** Wrong password or unknown email; the two are counted together, as the API answers them. */
  public void loginFailed() {
    registry.counter(LOGINS, OUTCOME, FAILURE).increment();
  }

  /** A used or revoked refresh token came back: possible theft, every session was revoked. */
  public void refreshTokenReused() {
    registry.counter(REFRESH_REUSE).increment();
  }

  /** {@code rule} is a {@code RateLimitRule} name, such as {@code LOGIN_IP}. */
  public void rateLimited(String rule) {
    registry.counter(RATE_LIMIT_REJECTIONS, "rule", rule.toLowerCase(Locale.ROOT)).increment();
  }

  /** {@code kind} is the email template, such as {@code verification} or {@code password-reset}. */
  public void mailSent(String kind, boolean success) {
    registry.counter(MAIL_SENT, "kind", kind, OUTCOME, success ? SUCCESS : FAILURE).increment();
  }

  /**
   * Times one call to the AI provider. The outcome is {@code success}, {@code unavailable} (the
   * provider is off or unreachable, a 503) or {@code error} (anything else, such as a reply that
   * could not be parsed).
   *
   * @param unavailable the exception type that means "provider unavailable"
   */
  public <T> T timeAi(
      String operation,
      String provider,
      Class<? extends RuntimeException> unavailable,
      Supplier<T> call) {
    long start = System.nanoTime();
    String outcome = "error";
    try {
      T result = call.get();
      outcome = SUCCESS;
      return result;
    } catch (RuntimeException e) {
      if (unavailable.isInstance(e)) {
        outcome = "unavailable";
      }
      throw e;
    } finally {
      Timer.builder(AI_REQUESTS)
          .tags("operation", operation, "provider", provider, OUTCOME, outcome)
          .publishPercentileHistogram()
          .register(registry)
          .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
  }

  /** Totals of one retention purge run (ADR-021). */
  public void retentionRun(int reminded, int deleted, int failures, long tokensDeleted) {
    retentionAccounts("reminded").increment(reminded);
    retentionAccounts("deleted").increment(deleted);
    retentionAccounts("failed").increment(failures);
    registry.counter(RETENTION_TOKENS_DELETED).increment(tokensDeleted);
  }

  private Counter retentionAccounts(String result) {
    return registry.counter(RETENTION_ACCOUNTS, "result", result);
  }
}
