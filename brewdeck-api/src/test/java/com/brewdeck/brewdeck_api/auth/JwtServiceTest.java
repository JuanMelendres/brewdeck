package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-secret-value-that-is-definitely-long-enough-1234";

  private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(24));

  private User user() {
    return User.builder().id(1L).email("brewer@example.com").passwordHash("x").build();
  }

  @Test
  void generateThenValidate_returnsSubjectEmail() {
    String token = jwtService.generateToken(user());

    assertThat(jwtService.validateAndGetSubject(token)).isEqualTo("brewer@example.com");
  }

  @Test
  void validate_rejectsTamperedToken() {
    String token = jwtService.generateToken(user());
    String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> jwtService.validateAndGetSubject(tampered))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void validate_rejectsTokenSignedWithDifferentSecret() {
    JwtService other =
        new JwtService("a-completely-different-secret-key-32bytes-minimum", Duration.ofHours(24));
    String token = other.generateToken(user());

    assertThatThrownBy(() -> jwtService.validateAndGetSubject(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void validate_rejectsExpiredToken() {
    JwtService expiring = new JwtService(SECRET, Duration.ofSeconds(-1));
    String token = expiring.generateToken(user());

    assertThatThrownBy(() -> jwtService.validateAndGetSubject(token))
        .isInstanceOf(JwtException.class);
  }
}
