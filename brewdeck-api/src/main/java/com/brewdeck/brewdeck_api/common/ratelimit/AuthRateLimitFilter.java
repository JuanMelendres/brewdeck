package com.brewdeck.brewdeck_api.common.ratelimit;

import com.brewdeck.brewdeck_api.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP limits on the public auth endpoints, applied before any controller work. Not a
 * Spring bean on purpose: it is added only to the security filter chain, so it runs once and is not
 * picked up by {@code @WebMvcTest} slices.
 *
 * <p>The IP is {@link HttpServletRequest#getRemoteAddr()}. Behind a reverse proxy, enable {@code
 * server.forward-headers-strategy} so that is the real client address. Raw {@code X-Forwarded-For}
 * is never trusted here because clients can spoof it.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final Map<String, RateLimitRule> RULES_BY_PATH =
      Map.of(
          "/api/auth/login", RateLimitRule.LOGIN_IP,
          "/api/auth/register", RateLimitRule.REGISTER_IP,
          "/api/auth/refresh", RateLimitRule.REFRESH_IP,
          "/api/auth/forgot-password", RateLimitRule.FORGOT_PASSWORD_IP,
          "/api/auth/reset-password", RateLimitRule.RESET_PASSWORD_IP,
          "/api/auth/verify-email", RateLimitRule.VERIFY_EMAIL_IP);

  private final RateLimiter rateLimiter;
  private final ObjectMapper objectMapper;

  public AuthRateLimitFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
    this.rateLimiter = rateLimiter;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !HttpMethod.POST.matches(request.getMethod()) || ruleFor(request) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    RateLimitRule rule = ruleFor(request);
    RateLimiter.Decision decision = rateLimiter.tryAcquire(rule, request.getRemoteAddr());
    if (decision.allowed()) {
      filterChain.doFilter(request, response);
      return;
    }

    long retryAfterSeconds = decision.retryAfterSeconds();
    ErrorResponse body =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
            RateLimitMessages.tooManyAttempts(retryAfterSeconds),
            request.getRequestURI(),
            null);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }

  private static RateLimitRule ruleFor(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return RULES_BY_PATH.get(path);
  }
}
