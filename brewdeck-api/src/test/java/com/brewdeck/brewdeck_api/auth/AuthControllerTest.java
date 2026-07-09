package com.brewdeck.brewdeck_api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Uses a standalone MockMvc wired with the real GlobalExceptionHandler so status mapping is
// exercised.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(
                new com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler())
            .build();
  }

  @Test
  void register_returns201WithToken() throws Exception {
    when(authService.register(any()))
        .thenReturn(
            new AuthResponse("jwt", Instant.parse("2026-07-09T00:00:00Z"), "new@example.com"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("new@example.com", "password1"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("jwt"))
        .andExpect(jsonPath("$.email").value("new@example.com"));
  }

  @Test
  void register_duplicateReturns409() throws Exception {
    when(authService.register(any()))
        .thenThrow(new EmailAlreadyUsedException("Email is already registered"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("taken@example.com", "password1"))))
        .andExpect(status().isConflict());
  }

  @Test
  void register_invalidEmailReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(new RegisterRequest("not-an-email", "short"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_badCredentialsReturns401() throws Exception {
    when(authService.login(any()))
        .thenThrow(new BadCredentialsException("Invalid email or password"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("brewer@example.com", "wrong"))))
        .andExpect(status().isUnauthorized());
  }
}
