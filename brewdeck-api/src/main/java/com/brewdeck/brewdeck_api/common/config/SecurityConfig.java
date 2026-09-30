package com.brewdeck.brewdeck_api.common.config;

import com.brewdeck.brewdeck_api.auth.JwtAuthenticationFilter;
import com.brewdeck.brewdeck_api.auth.Role;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationRequiredFilter;
import com.brewdeck.brewdeck_api.common.ratelimit.AuthRateLimitFilter;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;
  private final CorsConfigurationSource corsConfigurationSource;
  private final RateLimiter rateLimiter;
  private final ObjectMapper objectMapper;
  private final FeatureFlagService featureFlagService;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource,
      RateLimiter rateLimiter,
      ObjectMapper objectMapper,
      FeatureFlagService featureFlagService) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
    this.corsConfigurationSource = corsConfigurationSource;
    this.rateLimiter = rateLimiter;
    this.objectMapper = objectMapper;
    this.featureFlagService = featureFlagService;
  }

  // CSRF protection is intentionally disabled. This is a stateless, token-based REST
  // API: sessions are STATELESS and the JWT is sent as an Authorization: Bearer header,
  // never via a cookie or server session. CSRF attacks rely on ambient cookie credentials
  // the browser attaches automatically; a Bearer token must be set explicitly by the
  // client and is not exposed to CSRF. Disabling CSRF here follows Spring Security's
  // guidance for stateless APIs. Sonar java:S4502 is a false positive in this context.
  @Bean
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/verify-email")
                    .permitAll()
                    .requestMatchers("/api/public/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health")
                    .permitAll()
                    // Admin-only management APIs (e.g. the shared brew-method catalog).
                    .requestMatchers("/api/admin/**")
                    .hasRole(Role.ADMIN.name())
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        // Rate limiting runs first (after CORS) so throttled requests do no auth or DB work.
        .addFilterBefore(
            new AuthRateLimitFilter(rateLimiter, objectMapper),
            UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        // Needs the principal the JWT filter just set; blocks unverified users when flagged on.
        .addFilterAfter(
            new EmailVerificationRequiredFilter(featureFlagService, objectMapper),
            JwtAuthenticationFilter.class);
    return http.build();
  }
}
