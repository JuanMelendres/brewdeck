package com.brewdeck.brewdeck_api.account;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a user and everything they own, children first (ADR-020). Shared by "Delete my account"
 * and the retention purge (ADR-021), so both follow every new table that references users. The
 * shared brew-method catalog is untouched; email-change requests cascade from {@code users}.
 */
@Component
@RequiredArgsConstructor
public class AccountEraser {

  private final UserRepository userRepository;
  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;
  private final RecipeRepository recipeRepository;
  private final BrewSessionRepository brewSessionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;

  /** Runs inside the caller's transaction, so a failure rolls back the whole account. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void erase(User user) {
    Long id = user.getId();
    brewSessionRepository.deleteAllByOwnerId(id);
    recipeRepository.deleteAllByOwnerId(id);
    coffeeRepository.deleteAllByOwnerId(id);
    brewMethodRepository.deleteAllByOwnerId(id);
    refreshTokenRepository.deleteAllByUserId(id);
    passwordResetTokenRepository.deleteAllByUserId(id);
    emailVerificationTokenRepository.deleteAllByUserId(id);
    userRepository.delete(user);
    userRepository.flush();
  }
}
