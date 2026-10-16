package com.brewdeck.brewdeck_api.auth;

import io.jsonwebtoken.Claims;
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

  /** Claim holding {@link User#getTokenVersion()}; tokens without it count as version 0. */
  static final String TOKEN_VERSION_CLAIM = "ver";

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
        .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiryFor(issuedAt)))
        .signWith(key)
        .compact();
  }

  public VerifiedToken verify(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    Integer version = claims.get(TOKEN_VERSION_CLAIM, Integer.class);
    return new VerifiedToken(claims.getSubject(), version == null ? 0 : version);
  }

  public Instant expiryFor(Instant issuedAt) {
    return issuedAt.plus(ttl);
  }

  /** A signature- and expiry-checked access token: who it is for and which token version. */
  public record VerifiedToken(String subject, int tokenVersion) {}
}
