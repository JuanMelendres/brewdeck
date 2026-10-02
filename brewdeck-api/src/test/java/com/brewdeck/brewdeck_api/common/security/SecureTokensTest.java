package com.brewdeck.brewdeck_api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecureTokensTest {

  @Test
  void newTokenDecodesTo256BitsOfEntropy() {
    byte[] decoded = Base64.getUrlDecoder().decode(SecureTokens.newToken());

    // 32 bytes = 256 bits of entropy, base64url-encoded without padding.
    assertThat(decoded).hasSize(32);
  }

  @Test
  void newTokenIsUniquePerCall() {
    assertThat(SecureTokens.newToken()).isNotEqualTo(SecureTokens.newToken());
  }

  @Test
  void sha256HexIsLowercase64CharHexAndDeterministic() {
    String hash = SecureTokens.sha256Hex("a-raw-token");

    assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    // Deterministic for a given input (the persisted-hash lookup relies on this).
    assertThat(hash).isEqualTo(SecureTokens.sha256Hex("a-raw-token"));
    assertThat(hash).isNotEqualTo(SecureTokens.sha256Hex("different-token"));
  }

  @Test
  void newTokenWithLength_encodesThatManyBytes() {
    String shareSized = SecureTokens.newToken(16);

    assertThat(Base64.getUrlDecoder().decode(shareSized)).hasSize(16);
    // 16 bytes -> 22 base64url chars, which fits recipes.share_token VARCHAR(32).
    assertThat(shareSized).hasSize(22);
  }
}
