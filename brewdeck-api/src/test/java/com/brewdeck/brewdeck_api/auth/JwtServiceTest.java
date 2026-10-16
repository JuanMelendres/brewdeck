package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
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

    assertThat(jwtService.verify(token).subject()).isEqualTo("brewer@example.com");
  }

  @Test
  void validate_rejectsTamperedToken() {
    String token = jwtService.generateToken(user());
    String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> jwtService.verify(tampered)).isInstanceOf(JwtException.class);
  }

  @Test
  void validate_rejectsTokenSignedWithDifferentSecret() {
    JwtService other =
        new JwtService("a-completely-different-secret-key-32bytes-minimum", Duration.ofHours(24));
    String token = other.generateToken(user());

    assertThatThrownBy(() -> jwtService.verify(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void validate_rejectsExpiredToken() {
    JwtService expiring = new JwtService(SECRET, Duration.ofSeconds(-1));
    String token = expiring.generateToken(user());

    assertThatThrownBy(() -> jwtService.verify(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void generatedTokenCarriesTheUsersTokenVersion() {
    User user = user();
    user.setTokenVersion(3);

    assertThat(jwtService.verify(jwtService.generateToken(user)).tokenVersion()).isEqualTo(3);
  }

  @Test
  void tokenWithoutAVersionClaim_countsAsVersionZero() {
    // Tokens issued before V30 have no "ver" claim.
    String legacy =
        Jwts.builder()
            .subject("brewer@example.com")
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

    assertThat(jwtService.verify(legacy).tokenVersion()).isZero();
  }
}
