package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationMailPort;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationToken;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
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
class EmailVerificationIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private EmailVerificationTokenRepository tokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoSpyBean private EmailVerificationMailPort mailPort;

  @Test
  void registerThenVerify_flipsEmailVerified() throws Exception {
    String email = "verify-flow-" + System.nanoTime() + "@example.com";

    String registerResponse =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = com.jayway.jsonpath.JsonPath.read(registerResponse, "$.token");

    // Freshly registered user starts unverified.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(false));

    // The verification token was mailed on registration; capture it.
    ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(mailPort)
        .sendVerificationLink(org.mockito.ArgumentMatchers.eq(email), rawToken.capture());
    String verifyToken = rawToken.getValue();

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content("{\"token\":\"" + verifyToken + "\"}"))
        .andExpect(status().isNoContent());

    // Now verified.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true));

    // Single-use: replay -> 400.
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content("{\"token\":\"" + verifyToken + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resendVerification_authenticated_issuesNewToken() throws Exception {
    String email = "verify-resend-" + System.nanoTime() + "@example.com";
    String token =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.token");

    mockMvc
        .perform(post("/api/auth/resend-verification").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Verification email sent."));
  }

  @Test
  void verifyEmail_expiredToken_returns400() throws Exception {
    String email = "verify-exp-" + System.nanoTime() + "@example.com";
    User user =
        userRepository.save(
            User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("password1"))
                .createdAt(LocalDateTime.now())
                .build());
    String rawExpired = "expired-verify-token";
    tokenRepository.save(
        EmailVerificationToken.builder()
            .userId(user.getId())
            .tokenHash(sha256Hex(rawExpired))
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusHours(25))
            .build());

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content("{\"token\":\"" + rawExpired + "\"}"))
        .andExpect(status().isBadRequest());
  }

  private static String sha256Hex(String value) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    return java.util.HexFormat.of()
        .formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
