package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    authService = new AuthService(userRepository, jwtService, encoder);
  }

  private User stored(String email, String rawPassword) {
    return User.builder()
        .id(1L)
        .email(email)
        .passwordHash(new BCryptPasswordEncoder().encode(rawPassword))
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void register_persistsHashedPasswordAndReturnsToken() {
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response =
        authService.register(new RegisterRequest("new@example.com", "password1"));

    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.email()).isEqualTo("new@example.com");
  }

  @Test
  void register_throwsWhenEmailAlreadyUsed() {
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> authService.register(new RegisterRequest("taken@example.com", "password1")))
        .isInstanceOf(EmailAlreadyUsedException.class);
  }

  @Test
  void login_returnsTokenWhenPasswordMatches() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

    AuthResponse response = authService.login(new LoginRequest("brewer@example.com", "password1"));

    assertThat(response.token()).isEqualTo("jwt-token");
  }

  @Test
  void login_throwsWhenPasswordWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    assertThatThrownBy(() -> authService.login(new LoginRequest("brewer@example.com", "wrong")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void login_throwsWhenEmailUnknown() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "password1")))
        .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
  }

  @Test
  void me_returnsUserWhenPresent() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    UserResponse response = authService.me("brewer@example.com");

    assertThat(response.email()).isEqualTo("brewer@example.com");
  }

  @Test
  void me_throwsWhenMissing() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.me("ghost@example.com"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateProfile_setsDisplayNameAndReturnsUser() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UserResponse response =
        authService.updateProfile("brewer@example.com", new UpdateProfileRequest("Barista Bob"));

    assertThat(response.displayName()).isEqualTo("Barista Bob");
  }

  @Test
  void updateProfile_throwsWhenUserMissing() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService.updateProfile("ghost@example.com", new UpdateProfileRequest("X")))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void changePassword_reencodesWhenCurrentMatches() {
    User user = stored("brewer@example.com", "password1");
    String originalHash = user.getPasswordHash();
    when(userRepository.findByEmail("brewer@example.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    authService.changePassword(
        "brewer@example.com", new ChangePasswordRequest("password1", "newpassword1"));

    assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
    assertThat(new BCryptPasswordEncoder().matches("newpassword1", user.getPasswordHash()))
        .isTrue();
  }

  @Test
  void changePassword_throwsWhenCurrentWrong() {
    when(userRepository.findByEmail("brewer@example.com"))
        .thenReturn(Optional.of(stored("brewer@example.com", "password1")));

    assertThatThrownBy(
            () ->
                authService.changePassword(
                    "brewer@example.com", new ChangePasswordRequest("wrong", "newpassword1")))
        .isInstanceOf(InvalidCurrentPasswordException.class);
  }
}
