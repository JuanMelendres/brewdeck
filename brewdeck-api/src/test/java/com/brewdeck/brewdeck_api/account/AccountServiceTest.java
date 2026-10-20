package com.brewdeck.brewdeck_api.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.InvalidCurrentPasswordException;
import com.brewdeck.brewdeck_api.auth.Language;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitExceededException;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private UserRepository userRepository;
  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;
  @Mock private RecipeRepository recipeRepository;
  @Mock private BrewSessionRepository brewSessionRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RateLimiter rateLimiter;
  @Mock private AccountMailPort mailPort;

  @InjectMocks private AccountService service;

  private User user() {
    User user =
        User.builder()
            .id(9L)
            .email("brewer@example.com")
            .passwordHash("{argon2}hash")
            .language(Language.ES)
            .createdAt(Instant.now())
            .build();
    when(currentUserProvider.require()).thenReturn(user);
    return user;
  }

  @Test
  void delete_removesChildrenBeforeTheUser_thenSendsTheNotice() {
    User user = user();
    when(passwordEncoder.matches("brew-test-pass-123", "{argon2}hash")).thenReturn(true);

    service.delete(new DeleteAccountRequest("brew-test-pass-123"));

    InOrder order =
        inOrder(
            brewSessionRepository,
            recipeRepository,
            coffeeRepository,
            brewMethodRepository,
            refreshTokenRepository,
            passwordResetTokenRepository,
            emailVerificationTokenRepository,
            userRepository,
            mailPort);
    order.verify(brewSessionRepository).deleteAllByOwnerId(9L);
    order.verify(recipeRepository).deleteAllByOwnerId(9L);
    order.verify(coffeeRepository).deleteAllByOwnerId(9L);
    order.verify(brewMethodRepository).deleteAllByOwnerId(9L);
    order.verify(refreshTokenRepository).deleteAllByUserId(9L);
    order.verify(passwordResetTokenRepository).deleteAllByUserId(9L);
    order.verify(emailVerificationTokenRepository).deleteAllByUserId(9L);
    order.verify(userRepository).delete(user);
    order
        .verify(mailPort)
        .sendAccountDeletedNotice("brewer@example.com", Locale.forLanguageTag("es"));
  }

  @Test
  void delete_wrongPassword_deletesNothing() {
    user();
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    assertThatThrownBy(() -> service.delete(new DeleteAccountRequest("wrong-password-123")))
        .isInstanceOf(InvalidCurrentPasswordException.class);

    verify(rateLimiter).requireAllowed(RateLimitRule.ACCOUNT_DELETE_USER, "9");
    verifyNoInteractions(brewSessionRepository, recipeRepository, coffeeRepository, mailPort);
  }

  @Test
  void delete_overTheLimit_doesNotCheckThePassword() {
    user();
    doThrow(new RateLimitExceededException(Duration.ofMinutes(10)))
        .when(rateLimiter)
        .requireAllowed(RateLimitRule.ACCOUNT_DELETE_USER, "9");

    assertThatThrownBy(() -> service.delete(new DeleteAccountRequest("brew-test-pass-123")))
        .isInstanceOf(RateLimitExceededException.class);

    verifyNoInteractions(passwordEncoder, brewSessionRepository, mailPort);
  }

  @Test
  void export_isRateLimitedPerUser() {
    user();
    doThrow(new RateLimitExceededException(Duration.ofMinutes(30)))
        .when(rateLimiter)
        .requireAllowed(RateLimitRule.ACCOUNT_EXPORT_USER, "9");

    assertThatThrownBy(() -> service.export()).isInstanceOf(RateLimitExceededException.class);

    verifyNoInteractions(coffeeRepository, recipeRepository);
  }
}
