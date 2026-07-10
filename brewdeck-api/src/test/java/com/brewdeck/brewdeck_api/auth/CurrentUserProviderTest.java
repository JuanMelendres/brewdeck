package com.brewdeck.brewdeck_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
