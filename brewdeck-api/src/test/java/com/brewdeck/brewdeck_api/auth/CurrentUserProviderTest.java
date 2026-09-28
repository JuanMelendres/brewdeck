package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

  @Mock private UserRepository userRepository;

  private CurrentUserProvider currentUserProvider() {
    return new CurrentUserProvider(userRepository);
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void require_shouldReturnUser_whenPrincipalResolves() {
    authenticate("barista@brewdeck.test");
    User user = User.builder().id(7L).email("barista@brewdeck.test").build();
    when(userRepository.findByEmail("barista@brewdeck.test")).thenReturn(Optional.of(user));

    assertThat(currentUserProvider().require()).isSameAs(user);
  }

  @Test
  void require_shouldThrow_whenNoAuthentication() {
    SecurityContextHolder.clearContext();

    assertThatThrownBy(() -> currentUserProvider().require())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No authenticated user");
  }

  @Test
  void require_shouldThrow_whenPrincipalHasNoUser() {
    authenticate("ghost@brewdeck.test");
    when(userRepository.findByEmail("ghost@brewdeck.test")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> currentUserProvider().require())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no user");
  }

  private void authenticate(String email) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
  }

  @Test
  void require_withJwtPrincipal_returnsAReferenceWithoutQueryingByEmail() {
    User reference = User.builder().id(7L).build();
    when(userRepository.getReferenceById(7L)).thenReturn(reference);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(7L, "barista@brewdeck.test", Role.USER),
                null,
                java.util.List.of()));

    User result = currentUserProvider().require();

    assertThat(result.getId()).isEqualTo(7L);
    verify(userRepository, never()).findByEmail(anyString());
  }

  @Test
  void authenticatedUser_nameIsTheEmail() {
    AuthenticatedUser principal = new AuthenticatedUser(7L, "barista@brewdeck.test", Role.ADMIN);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());

    // Controllers that take java.security.Principal keep receiving the email.
    assertThat(principal.getName()).isEqualTo("barista@brewdeck.test");
    assertThat(authentication.getName()).isEqualTo("barista@brewdeck.test");
  }
}
