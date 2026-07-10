package com.brewdeck.brewdeck_api.common;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
}
