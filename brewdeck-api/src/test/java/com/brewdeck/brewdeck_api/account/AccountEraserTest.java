package com.brewdeck.brewdeck_api.account;

import static org.mockito.Mockito.inOrder;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountEraserTest {

  @Mock private UserRepository userRepository;
  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;
  @Mock private RecipeRepository recipeRepository;
  @Mock private BrewSessionRepository brewSessionRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;

  @InjectMocks private AccountEraser eraser;

  @Test
  void erase_removesChildrenBeforeTheUser() {
    User user = User.builder().id(9L).email("brewer@example.com").build();

    eraser.erase(user);

    InOrder order =
        inOrder(
            brewSessionRepository,
            recipeRepository,
            coffeeRepository,
            brewMethodRepository,
            refreshTokenRepository,
            passwordResetTokenRepository,
            emailVerificationTokenRepository,
            userRepository);
    order.verify(brewSessionRepository).deleteAllByOwnerId(9L);
    order.verify(recipeRepository).deleteAllByOwnerId(9L);
    order.verify(coffeeRepository).deleteAllByOwnerId(9L);
    order.verify(brewMethodRepository).deleteAllByOwnerId(9L);
    order.verify(refreshTokenRepository).deleteAllByUserId(9L);
    order.verify(passwordResetTokenRepository).deleteAllByUserId(9L);
    order.verify(emailVerificationTokenRepository).deleteAllByUserId(9L);
    order.verify(userRepository).delete(user);
    order.verify(userRepository).flush();
  }
}
