package com.brewdeck.brewdeck_api.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brewdeck.brewdeck_api.common.i18n.TestMessages;
import com.brewdeck.brewdeck_api.common.metrics.BrewdeckMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class RateLimiterTest {

  /** A clock the test can move forward. */
  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-09-26T10:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  private final MutableClock clock = new MutableClock();
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final BrewdeckMetrics metrics = new BrewdeckMetrics(meterRegistry);
  private final RateLimiter limiter = new RateLimiter(true, clock, metrics);

  private void exhaust(RateLimitRule rule, String key) {
    for (int i = 0; i < rule.limit(); i++) {
      assertThat(limiter.tryAcquire(rule, key).allowed()).isTrue();
    }
  }

  @Test
  void allowsUpToTheLimitThenBlocksWithTimeLeftInTheWindow() {
    exhaust(RateLimitRule.LOGIN_EMAIL, "a@x.com");
    clock.advance(Duration.ofMinutes(5));

    RateLimiter.Decision blocked = limiter.tryAcquire(RateLimitRule.LOGIN_EMAIL, "a@x.com");

    assertThat(blocked.allowed()).isFalse();
    assertThat(blocked.retryAfter()).isEqualTo(Duration.ofMinutes(10));
    assertThat(blocked.retryAfterSeconds()).isEqualTo(600);
  }

  @Test
  void rejectionsAreCountedByRule_withoutTheKey() {
    exhaust(RateLimitRule.LOGIN_EMAIL, "a@x.com");

    limiter.tryAcquire(RateLimitRule.LOGIN_EMAIL, "a@x.com");
    limiter.tryAcquire(RateLimitRule.LOGIN_EMAIL, "a@x.com");

    assertThat(
            meterRegistry.counter("brewdeck.rate.limit.rejections", "rule", "login_email").count())
        .isEqualTo(2);
    // The key (an email or IP) never becomes a tag.
    assertThat(meterRegistry.find("brewdeck.rate.limit.rejections").meters())
        .singleElement()
        .satisfies(meter -> assertThat(meter.getId().getTags()).hasSize(1));
  }

  @Test
  void opensAgainWhenTheWindowEnds() {
    exhaust(RateLimitRule.LOGIN_EMAIL, "a@x.com");
    assertThat(limiter.tryAcquire(RateLimitRule.LOGIN_EMAIL, "a@x.com").allowed()).isFalse();

    clock.advance(RateLimitRule.LOGIN_EMAIL.window());

    assertThat(limiter.tryAcquire(RateLimitRule.LOGIN_EMAIL, "a@x.com").allowed()).isTrue();
  }

  @Test
  void keysAndRulesAreIndependent() {
    exhaust(RateLimitRule.FORGOT_PASSWORD_EMAIL, "a@x.com");

    assertThat(limiter.tryAcquire(RateLimitRule.FORGOT_PASSWORD_EMAIL, "b@x.com").allowed())
        .isTrue();
    assertThat(limiter.tryAcquire(RateLimitRule.LOGIN_EMAIL, "a@x.com").allowed()).isTrue();
  }

  @Test
  void requireAllowed_throwsOnceOverTheLimit() {
    exhaust(RateLimitRule.REGISTER_IP, "10.0.0.1");

    assertThatThrownBy(() -> limiter.requireAllowed(RateLimitRule.REGISTER_IP, "10.0.0.1"))
        .isInstanceOf(RateLimitExceededException.class);
  }

  @Test
  void disabledLimiterAlwaysAllows() {
    RateLimiter disabled = new RateLimiter(false, clock, metrics);

    for (int i = 0; i < 100; i++) {
      assertThat(disabled.tryAcquire(RateLimitRule.REGISTER_IP, "10.0.0.1").allowed()).isTrue();
    }
  }

  @Test
  void retryAfterSecondsRoundsUpAndIsAtLeastOne() {
    assertThat(new RateLimiter.Decision(false, Duration.ofMillis(1500)).retryAfterSeconds())
        .isEqualTo(2);
    assertThat(new RateLimiter.Decision(false, Duration.ZERO).retryAfterSeconds()).isEqualTo(1);
  }

  @Test
  void messageUsesWholeMinutes() {
    MessageSource messages = TestMessages.messageSource();
    assertThat(RateLimitMessages.tooManyAttempts(messages, Locale.ENGLISH, 30))
        .isEqualTo("Too many attempts. Try again in a minute.");
    assertThat(RateLimitMessages.tooManyAttempts(messages, Locale.ENGLISH, 61))
        .isEqualTo("Too many attempts. Try again in 2 minutes.");
  }

  @Test
  void messageIsInTheRequestLanguage() {
    MessageSource messages = TestMessages.messageSource();
    assertThat(RateLimitMessages.tooManyAttempts(messages, Locale.forLanguageTag("es"), 61))
        .isEqualTo("Demasiados intentos. Inténtalo de nuevo en 2 minutos.");
  }
}
