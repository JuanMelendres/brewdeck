package com.brewdeck.brewdeck_api.auth.refresh;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * The refresh token travels in an httpOnly cookie so page scripts (and any XSS) can never read it
 * (ADR-013). It is Secure, SameSite=Strict, and scoped to {@code /api/auth}, so browsers send it
 * only to the auth endpoints and never on cross-site requests.
 *
 * <p>Because a cookie is sent automatically, a request that authenticates with it must also carry
 * {@code X-Requested-With} (defense in depth against CSRF on top of SameSite; a cross-site page
 * cannot add that header without a CORS preflight the API refuses).
 *
 * <p>Transition (ADR-013): the token may still come in the JSON body for clients that have not
 * moved to the cookie yet. A cookie always wins over the body.
 */
@Component
public class RefreshTokenCookies {

  public static final String CSRF_HEADER = "X-Requested-With";

  private final String name;
  private final String path;
  private final boolean secure;
  private final String sameSite;
  private final Duration maxAge;

  public RefreshTokenCookies(
      @Value("${brewdeck.auth.refresh-cookie.name:brewdeck_refresh}") String name,
      @Value("${brewdeck.auth.refresh-cookie.path:/api/auth}") String path,
      @Value("${brewdeck.auth.refresh-cookie.secure:true}") boolean secure,
      @Value("${brewdeck.auth.refresh-cookie.same-site:Strict}") String sameSite,
      @Value("${brewdeck.auth.refresh-ttl}") Duration maxAge) {
    this.name = name;
    this.path = path;
    this.secure = secure;
    this.sameSite = sameSite;
    this.maxAge = maxAge;
  }

  /** Set-Cookie value carrying a newly issued refresh token. */
  public ResponseCookie issue(String rawToken) {
    return base(rawToken).maxAge(maxAge).build();
  }

  /** Set-Cookie value that deletes the cookie. */
  public ResponseCookie clear() {
    return base("").maxAge(Duration.ZERO).build();
  }

  /**
   * The refresh token presented by the client: the cookie if present (requiring the CSRF header),
   * otherwise the legacy body value. {@code null} when neither is present.
   */
  public String resolve(HttpServletRequest request, RefreshRequest body) {
    String fromCookie = cookieValue(request);
    if (fromCookie != null) {
      String csrfHeader = request.getHeader(CSRF_HEADER);
      if (csrfHeader == null || csrfHeader.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Missing " + CSRF_HEADER + " header");
      }
      return fromCookie;
    }
    return body == null ? null : body.refreshToken();
  }

  private String cookieValue(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    return Arrays.stream(cookies)
        .filter(cookie -> name.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(path);
  }
}
