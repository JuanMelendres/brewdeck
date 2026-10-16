package com.brewdeck.brewdeck_api.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start with settings that are only safe on a developer machine (audit AUTH-05).
 *
 * <ul>
 *   <li>The dev JWT secret from {@code application.yaml} is public, so anyone could forge access
 *       tokens with it. It is accepted only under the {@code local} or {@code test} profile; any
 *       other profile (or none) must set {@code BREWDECK_JWT_SECRET}.
 *   <li>With {@code brewdeck.mail.enabled=false} the logging mail adapters write full password
 *       reset and email verification links to the logs. Hosted {@code staging} and {@code prod}
 *       must send real email instead.
 * </ul>
 */
@Component
public class StartupSafetyCheck {

  /** Must match the {@code brewdeck.auth.secret} fallback in {@code application.yaml}. */
  static final String DEV_JWT_SECRET = "dev-only-insecure-secret-change-me-at-least-32-bytes";

  private static final Profiles DEV_PROFILES = Profiles.of("local | test");
  private static final Profiles HOSTED_PROFILES = Profiles.of("staging | prod");

  public StartupSafetyCheck(
      Environment environment,
      @Value("${brewdeck.auth.secret}") String jwtSecret,
      @Value("${brewdeck.mail.enabled}") boolean mailEnabled) {
    if (DEV_JWT_SECRET.equals(jwtSecret) && !environment.acceptsProfiles(DEV_PROFILES)) {
      throw new IllegalStateException(
          "brewdeck.auth.secret is the public dev default; set BREWDECK_JWT_SECRET (at least 32"
              + " bytes) or run with the local profile");
    }
    if (!mailEnabled && environment.acceptsProfiles(HOSTED_PROFILES)) {
      throw new IllegalStateException(
          "brewdeck.mail.enabled is false, which logs password reset and verification links; set"
              + " BREWDECK_MAIL_ENABLED=true and SPRING_MAIL_* in staging and prod");
    }
  }
}
