package com.brewdeck.brewdeck_api.auth.verification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EmailVerificationControllerTest {

  @Mock private EmailVerificationService emailVerificationService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new EmailVerificationController(emailVerificationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void verifyEmail_returns204() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("raw-token"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void verifyEmail_invalidTokenReturns400() throws Exception {
    doThrow(new InvalidVerificationTokenException("bad"))
        .when(emailVerificationService)
        .verify(any());

    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("raw-token"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void verifyEmail_blankTokenReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest(""))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resendVerification_returns200WithMessage() throws Exception {
    Principal principal = () -> "brewer@example.com";

    mockMvc
        .perform(post("/api/auth/resend-verification").principal(principal))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Verification email sent."));
  }
}
