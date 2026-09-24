package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

  private static final String ADMIN_EMAIL = "owner@example.com";

  @Mock private UserRepository userRepository;

  private User user(Role role) {
    return User.builder()
        .id(1L)
        .email(ADMIN_EMAIL)
        .passwordHash("hash")
        .role(role)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void run_blankEmail_doesNothing() {
    new AdminBootstrap(userRepository, "  ").run(null);

    verifyNoInteractions(userRepository);
  }

  @Test
  void run_existingRegularUser_isPromotedToAdmin() {
    User user = user(Role.USER);
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(user));

    new AdminBootstrap(userRepository, ADMIN_EMAIL).run(null);

    assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    verify(userRepository).save(user);
  }

  @Test
  void run_alreadyAdmin_isNotSavedAgain() {
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(user(Role.ADMIN)));

    new AdminBootstrap(userRepository, ADMIN_EMAIL).run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void run_unknownEmail_createsNoAccount() {
    when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());

    new AdminBootstrap(userRepository, ADMIN_EMAIL).run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void newUser_defaultsToRegularRole() {
    User user = User.builder().email("new@example.com").passwordHash("hash").build();

    assertThat(user.getRole()).isEqualTo(Role.USER);
  }
}
