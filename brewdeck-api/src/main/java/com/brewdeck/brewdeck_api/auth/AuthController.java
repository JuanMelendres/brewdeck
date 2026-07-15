package com.brewdeck.brewdeck_api.auth;

import com.brewdeck.brewdeck_api.auth.refresh.RefreshRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, and current-user lookup")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a new account")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Log in and receive a bearer token")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
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
  @Operation(summary = "Exchange a refresh token for a new access + refresh token pair")
  public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Revoke the presented refresh token")
  public void logout(Principal principal, @Valid @RequestBody RefreshRequest request) {
    authService.logout(principal.getName(), request);
  }
}
