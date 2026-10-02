package com.brewdeck.brewdeck_api.auth.verification;

import com.brewdeck.brewdeck_api.auth.AuthenticatedUser;
import com.brewdeck.brewdeck_api.common.error.ErrorResponse;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * When the {@code auth-require-email-verification} flag is on, an authenticated user whose email is
 * not verified gets {@code 403 EMAIL_NOT_VERIFIED} on every endpoint except the few needed to
 * finish verifying (ADR-012). Runs right after the JWT filter, before any controller or side
 * effect, so the backend is the source of truth rather than the UI.
 *
 * <p>Verified users and anonymous requests pass straight through, and the flag is only evaluated
 * for unverified users (a cached lookup). Not a Spring bean on purpose: it is added only to the
 * security chain, like {@code AuthRateLimitFilter}.
 */
public class EmailVerificationRequiredFilter extends OncePerRequestFilter {

  public static final String CODE = "EMAIL_NOT_VERIFIED";

  /** "METHOD path" pairs an unverified user may still call. */
  private static final Set<String> ALLOWED =
      Set.of(
          "GET /api/auth/me",
          "POST /api/auth/resend-verification",
          "POST /api/auth/verify-email",
          "POST /api/auth/logout",
          "POST /api/auth/refresh",
          "GET /api/feature-flags");

  private final FeatureFlagService featureFlagService;
  private final ObjectMapper objectMapper;

  public EmailVerificationRequiredFilter(
      FeatureFlagService featureFlagService, ObjectMapper objectMapper) {
    this.featureFlagService = featureFlagService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (mustBlock(request)) {
      writeForbidden(request, response);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean mustBlock(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
        || user.emailVerified()) {
      return false;
    }
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (ALLOWED.contains(request.getMethod() + " " + path)) {
      return false;
    }
    return featureFlagService.isEnabled(FeatureKeys.REQUIRE_EMAIL_VERIFICATION);
  }

  private void writeForbidden(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ErrorResponse body =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "Verify your email address to continue",
            request.getRequestURI(),
            null,
            CODE);
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
