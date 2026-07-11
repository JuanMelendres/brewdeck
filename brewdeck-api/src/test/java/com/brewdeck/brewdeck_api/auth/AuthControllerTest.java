package com.brewdeck.brewdeck_api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
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

  @Test
  void updateProfile_returns200WithUpdatedName() throws Exception {
    Principal principal = () -> "brewer@example.com";
    when(authService.updateProfile(eq("brewer@example.com"), any()))
        .thenReturn(
            new UserResponse(
                1L, "brewer@example.com", "Barista Bob", LocalDateTime.parse("2026-07-09T00:00")));

    mockMvc
        .perform(
            patch("/api/auth/me")
                .principal(principal)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Barista Bob"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Barista Bob"));
  }

  @Test
  void changePassword_returns204() throws Exception {
    Principal principal = () -> "brewer@example.com";

    mockMvc
        .perform(
            post("/api/auth/change-password")
                .principal(principal)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ChangePasswordRequest("password1", "newpassword1"))))
        .andExpect(status().isNoContent());
  }

  @Test
  void changePassword_wrongCurrentReturns400() throws Exception {
    Principal principal = () -> "brewer@example.com";
    doThrow(new InvalidCurrentPasswordException("Current password is incorrect"))
        .when(authService)
        .changePassword(eq("brewer@example.com"), any());

    mockMvc
        .perform(
            post("/api/auth/change-password")
                .principal(principal)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ChangePasswordRequest("wrong", "newpassword1"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changePassword_shortNewPasswordReturns400() throws Exception {
    Principal principal = () -> "brewer@example.com";

    mockMvc
        .perform(
            post("/api/auth/change-password")
                .principal(principal)
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new ChangePasswordRequest("password1", "short"))))
        .andExpect(status().isBadRequest());
  }
}
