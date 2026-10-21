package com.brewdeck.brewdeck_api.account;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.InvalidCurrentPasswordException;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserLocale;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.coffee.CoffeeResponse;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.method.BrewMethodResponse;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.recipe.RecipeResponse;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.brewdeck.brewdeck_api.session.BrewSessionResponse;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service data export and account deletion (ADR-020, ARCO access and cancellation). */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

  private final CurrentUserProvider currentUserProvider;
  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;
  private final RecipeRepository recipeRepository;
  private final BrewSessionRepository brewSessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final RateLimiter rateLimiter;
  private final AccountMailPort mailPort;
  private final AccountEraser accountEraser;

  @Transactional(readOnly = true)
  public AccountExportResponse export() {
    User user = currentUserProvider.require();
    rateLimiter.requireAllowed(RateLimitRule.ACCOUNT_EXPORT_USER, String.valueOf(user.getId()));
    Long id = user.getId();
    AccountExportResponse export =
        new AccountExportResponse(
            Instant.now(),
            ProfileExport.fromEntity(user),
            coffeeRepository.findAllByOwnerIdOrderByIdAsc(id).stream()
                .map(CoffeeResponse::fromEntity)
                .toList(),
            brewMethodRepository.findAllByOwnerIdOrderByIdAsc(id).stream()
                .map(BrewMethodResponse::fromEntity)
                .toList(),
            recipeRepository.findAllByOwnerIdOrderByIdAsc(id).stream()
                .map(RecipeResponse::fromEntity)
                .toList(),
            brewSessionRepository.findAllByOwnerIdOrderByIdAsc(id).stream()
                .map(BrewSessionResponse::fromEntity)
                .toList());
    log.info("Exported data for user id={}", id);
    return export;
  }

  /**
   * Deletes the current user and everything they own in one transaction ({@link AccountEraser}).
   */
  @Transactional
  public void delete(DeleteAccountRequest request) {
    User user = currentUserProvider.require();
    // Before the password check, so wrong guesses with a stolen access token count.
    rateLimiter.requireAllowed(RateLimitRule.ACCOUNT_DELETE_USER, String.valueOf(user.getId()));
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidCurrentPasswordException("Current password is incorrect");
    }
    Long id = user.getId();
    String email = user.getEmail();
    Locale locale = UserLocale.of(user);

    accountEraser.erase(user);

    // The SMTP adapter sends after the commit, so a rolled-back delete emails nothing.
    mailPort.sendAccountDeletedNotice(email, locale);
    log.info("Deleted account id={}", id);
  }
}
