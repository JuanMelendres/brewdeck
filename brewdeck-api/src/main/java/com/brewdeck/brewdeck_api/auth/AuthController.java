package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.auth.refresh.RefreshRequest;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenCookies refreshTokenCookies;

  @PostMapping("/register")
  @Operation(
      summary = "Register a new account",
      description = "Returns the access token; the refresh token is set as an httpOnly cookie.")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return withRefreshCookie(HttpStatus.CREATED, authService.register(request));
  }

  @PostMapping("/login")
  @Operation(
      summary = "Log in and receive a bearer token",
      description = "Returns the access token; the refresh token is set as an httpOnly cookie.")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return withRefreshCookie(HttpStatus.OK, authService.login(request));
  }

  @GetMapping("/me")
  @Operation(summary = "Get the currently authenticated user")
  public ResponseEntity<UserResponse> me(Principal principal) {
    return ResponseEntity.ok(authService.me(principal.getName()));
  }

  @PatchMapping("/me")
  @Operation(summary = "Update the authenticated user's profile")
  public ResponseEntity<UserResponse> updateProfile(
      Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(authService.updateProfile(principal.getName(), request));
  }

  @PostMapping("/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Change the authenticated user's password")
  public void changePassword(
      Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(principal.getName(), request);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Exchange the refresh token for a new access token and rotated refresh cookie",
      description =
          "Reads the refresh token from the httpOnly cookie (requires the X-Requested-With"
              + " header) or, during the transition, from the JSON body.")
  public ResponseEntity<AuthResponse> refresh(
      HttpServletRequest httpRequest, @RequestBody(required = false) RefreshRequest body) {
    String rawToken = refreshTokenCookies.resolve(httpRequest, body);
    return withRefreshCookie(HttpStatus.OK, authService.refresh(rawToken));
  }

  @PostMapping("/logout")
  @Operation(summary = "Revoke the presented refresh token and clear the refresh cookie")
  public ResponseEntity<Void> logout(
      Principal principal,
      HttpServletRequest httpRequest,
      @RequestBody(required = false) RefreshRequest body) {
    authService.logout(principal.getName(), refreshTokenCookies.resolve(httpRequest, body));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
        .build();
  }

  private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, AuthResponse auth) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.issue(auth.refreshToken()).toString())
        .body(auth);
  }
}
