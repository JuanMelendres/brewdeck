package com.brewdeck.brewdeck_api.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grants {@link Role#ADMIN} to the account named by {@code brewdeck.auth.admin-email} at startup.
 * This is the only way to become an admin: no endpoint can grant roles.
 *
 * <p>Promotion runs only for an account that already exists. The operator registers the account
 * first, then sets {@code BREWDECK_ADMIN_EMAIL} and restarts — so nobody can pre-register the
 * configured address to squat the admin role. The account must also have verified its email:
 * otherwise anyone could register the owner's address before the owner does and be promoted on the
 * next restart. Removing the variable does not demote anyone.
 */
@Service
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

  private final UserRepository userRepository;
  private final String adminEmail;

  public AdminBootstrap(
      UserRepository userRepository, @Value("${brewdeck.auth.admin-email:}") String adminEmail) {
    this.userRepository = userRepository;
    this.adminEmail = adminEmail;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (adminEmail == null || adminEmail.isBlank()) {
      return;
    }
    userRepository
        .findByEmail(EmailAddresses.normalize(adminEmail))
        .ifPresentOrElse(
            user -> {
              if (!user.isEmailVerified()) {
                log.warn(
                    "brewdeck.auth.admin-email names an account whose email is not verified;"
                        + " verify it first, then restart");
                return;
              }
              if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                log.info("Granted ADMIN role to user id={}", user.getId());
              }
            },
            () ->
                log.warn(
                    "brewdeck.auth.admin-email is set but no account exists for it; register it"
                        + " first, then restart"));
  }
}
