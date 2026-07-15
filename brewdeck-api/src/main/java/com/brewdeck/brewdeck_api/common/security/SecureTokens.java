package com.brewdeck.brewdeck_api.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Shared helpers for opaque security tokens (password reset, email verification): generate a
 * high-entropy raw token and derive the SHA-256 hex hash that is persisted. Only the hash is
 * stored; the raw token travels solely to the user (e.g. via an emailed link).
 */
public final class SecureTokens {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private SecureTokens() {}

  /** A new 256-bit random token, base64url-encoded without padding. */
  public static String newToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** The lowercase hex SHA-256 of a raw token — the value persisted in the DB. */
  public static String sha256Hex(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
