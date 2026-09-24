package com.brewdeck.brewdeck_api.common;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class PostgresIntegrationTest {

  /**
   * Matches the default principal name of {@code @WithMockUser}, so ownership-stamping create paths
   * can resolve the authenticated user against a real row in the shared integration database.
   */
  protected static final String MOCK_USER_EMAIL = "user";

  @SuppressWarnings("resource")
  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("brewdeck_integration_test")
          .withUsername("brewdeck")
          .withPassword("brewdeck");

  static {
    POSTGRES.start();
  }

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void seedMockUser() {
    if (userRepository.findByEmail(MOCK_USER_EMAIL).isEmpty()) {
      userRepository.save(
          User.builder()
              .email(MOCK_USER_EMAIL)
              .passwordHash("integration-test-placeholder")
              .createdAt(LocalDateTime.now())
              .build());
    }
  }

  /**
   * Runs a request as the seeded mock user holding {@code ROLE_ADMIN}, for admin-only endpoints
   * such as brew-method writes.
   */
  protected static RequestPostProcessor asAdmin() {
    return SecurityMockMvcRequestPostProcessors.user(MOCK_USER_EMAIL).roles("ADMIN");
  }

  protected User mockUser() {
    return userRepository
        .findByEmail(MOCK_USER_EMAIL)
        .orElseThrow(() -> new IllegalStateException("mock user not seeded"));
  }
}
