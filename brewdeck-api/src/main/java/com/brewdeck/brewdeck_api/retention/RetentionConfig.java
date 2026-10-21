package com.brewdeck.brewdeck_api.retention;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RetentionConfig {

  /** The purge's notion of "now"; tests call {@code RetentionPurgeService.run(Instant)} instead. */
  @Bean
  Clock retentionClock() {
    return Clock.systemUTC();
  }
}
