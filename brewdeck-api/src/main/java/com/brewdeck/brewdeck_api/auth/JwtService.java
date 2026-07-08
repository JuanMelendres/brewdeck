package com.brewdeck.brewdeck_api.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration ttl;

  public JwtService(
      @Value("${brewdeck.auth.secret}") String secret,
      @Value("${brewdeck.auth.token-ttl}") Duration ttl) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.ttl = ttl;
  }

  public String generateToken(User user) {
    Instant issuedAt = Instant.now();
    return Jwts.builder()
        .subject(user.getEmail())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiryFor(issuedAt)))
        .signWith(key)
        .compact();
  }

  public String validateAndGetSubject(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public Instant expiryFor(Instant issuedAt) {
    return issuedAt.plus(ttl);
  }
}
