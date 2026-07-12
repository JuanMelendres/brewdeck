package com.brewdeck.brewdeck_api.auth.reset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

  @Mock private PasswordResetService passwordResetService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PasswordResetController(passwordResetService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void forgotPassword_returns200GenericMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("a@b.com"))))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message").value("If that email exists, a reset link has been sent."));
  }

  @Test
  void forgotPassword_invalidEmailReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("not-email"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resetPassword_returns204() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ResetPasswordRequest("raw-token", "newpassword1"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void resetPassword_invalidTokenReturns400() throws Exception {
    doThrow(new InvalidResetTokenException("bad")).when(passwordResetService).resetPassword(any());

    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ResetPasswordRequest("raw-token", "newpassword1"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void resetPassword_shortPasswordReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ResetPasswordRequest("raw-token", "short"))))
        .andExpect(status().isBadRequest());
  }
}
