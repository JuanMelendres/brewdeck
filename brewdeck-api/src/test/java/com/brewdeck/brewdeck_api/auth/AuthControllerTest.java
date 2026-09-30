package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.refresh.InvalidRefreshTokenException;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenCookies;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
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
        MockMvcBuilders.standaloneSetup(
                new AuthController(
                    authService,
                    new RefreshTokenCookies(
                        "brewdeck_refresh", "/api/auth", true, "Strict", Duration.ofDays(7))))
            .setControllerAdvice(
                new com.brewdeck.brewdeck_api.common.error.GlobalExceptionHandler())
            .build();
  }

  @Test
  void register_returns201WithToken() throws Exception {
    when(authService.register(any()))
        .thenReturn(
            new AuthResponse(
                "jwt", Instant.parse("2026-07-09T00:00:00Z"), "new@example.com", "refresh-token"));

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
                1L,
                "brewer@example.com",
                "Barista Bob",
                true,
                Role.USER,
                Instant.parse("2026-07-09T00:00:00Z")));

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

  @Test
  void refreshReturns200WithNewPair() throws Exception {
    when(authService.refresh("old-refresh"))
        .thenReturn(
            new AuthResponse(
                "new-jwt", Instant.parse("2026-07-14T00:15:00Z"), "u@example.com", "new-refresh"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"old-refresh\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("new-jwt"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
        .andExpect(jsonPath("$.email").value("u@example.com"));
  }

  @Test
  void refreshReturns401OnInvalidToken() throws Exception {
    when(authService.refresh("bad")).thenThrow(new InvalidRefreshTokenException("bad"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content("{\"refreshToken\":\"bad\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token is invalid or has expired"));
  }

  @Test
  void refreshReturns401WhenNoTokenIsPresented() throws Exception {
    when(authService.refresh(null)).thenThrow(new InvalidRefreshTokenException("Missing"));

    mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
  }

  @Test
  void login_setsTheRefreshTokenAsAHardenedCookie() throws Exception {
    when(authService.login(any()))
        .thenReturn(
            new AuthResponse("jwt", Instant.parse("2026-07-09T00:00:00Z"), "u@example.com", "r1"));

    String setCookie =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"email\":\"u@example.com\",\"password\":\"password1\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);

    assertThat(setCookie)
        .startsWith("brewdeck_refresh=r1")
        .contains("Path=/api/auth")
        .contains("Max-Age=604800")
        .contains("Secure")
        .contains("HttpOnly")
        .contains("SameSite=Strict");
  }

  @Test
  void refresh_prefersTheCookieOverTheBody_andRotatesIt() throws Exception {
    when(authService.refresh("from-cookie"))
        .thenReturn(
            new AuthResponse("jwt", Instant.parse("2026-07-09T00:00:00Z"), "u@example.com", "r2"));

    String setCookie =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .cookie(new Cookie("brewdeck_refresh", "from-cookie"))
                    .header("X-Requested-With", "fetch")
                    .contentType("application/json")
                    .content("{\"refreshToken\":\"from-body\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);

    assertThat(setCookie).startsWith("brewdeck_refresh=r2");
    verify(authService, never()).refresh("from-body");
  }

  @Test
  void refresh_withCookieButNoCsrfHeader_isForbidden() throws Exception {
    mockMvc
        .perform(post("/api/auth/refresh").cookie(new Cookie("brewdeck_refresh", "from-cookie")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Missing X-Requested-With header"));

    verify(authService, never()).refresh(anyString());
  }

  @Test
  void logout_revokesTheCookieTokenAndClearsTheCookie() throws Exception {
    String setCookie =
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .principal(() -> "u@example.com")
                    .cookie(new Cookie("brewdeck_refresh", "from-cookie"))
                    .header("X-Requested-With", "fetch"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);

    verify(authService).logout("u@example.com", "from-cookie");
    assertThat(setCookie).startsWith("brewdeck_refresh=;").contains("Max-Age=0");
  }

  @Test
  void logoutReturns204() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/logout")
                .principal(() -> "u@example.com")
                .contentType("application/json")
                .content("{\"refreshToken\":\"some-refresh\"}"))
        .andExpect(status().isNoContent());

    verify(authService).logout(eq("u@example.com"), eq("some-refresh"));
  }
}
