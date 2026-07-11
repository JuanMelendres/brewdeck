package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetMailPort;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetToken;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordResetTokenRepository tokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoSpyBean private PasswordResetMailPort mailPort;

  @Test
  void fullFlow_forgotThenResetThenLogin() throws Exception {
    String email = "reset-flow-" + System.nanoTime() + "@example.com";
    userRepository.save(
        User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode("password1"))
            .createdAt(LocalDateTime.now())
            .build());

    // forgot-password returns 200 and issues a link
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk());

    ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(mailPort)
        .sendResetLink(org.mockito.ArgumentMatchers.eq(email), rawToken.capture());
    String token = rawToken.getValue();

    // reset-password succeeds (204)
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isNoContent());

    // new password logs in, old one does not
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"newpassword1\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
        .andExpect(status().isUnauthorized());

    // token is single-use: replay returns 400
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"another12\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void forgotPassword_unknownEmail_returns200AndSendsNothing() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\":\"nobody-" + System.nanoTime() + "@example.com\"}"))
        .andExpect(status().isOk());

    org.mockito.Mockito.verify(mailPort, org.mockito.Mockito.never())
        .sendResetLink(
            org.mockito.ArgumentMatchers.contains("nobody-"),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void resetPassword_expiredToken_returns400() throws Exception {
    String email = "reset-exp-" + System.nanoTime() + "@example.com";
    User user =
        userRepository.save(
            User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password1"))
                .createdAt(LocalDateTime.now())
                .build());
    // Persist a token whose hash we know, already expired.
    // SHA-256 hex of "expired-raw-token":
    String rawExpired = "expired-raw-token";
    tokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(sha256Hex(rawExpired))
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusMinutes(31))
            .build());

    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\":\"" + rawExpired + "\",\"newPassword\":\"newpassword1\"}"))
        .andExpect(status().isBadRequest());
  }

  private static String sha256Hex(String value) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    return java.util.HexFormat.of()
        .formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
