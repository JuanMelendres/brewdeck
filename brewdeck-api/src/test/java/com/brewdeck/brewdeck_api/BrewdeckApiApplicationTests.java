package com.brewdeck.brewdeck_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BrewdeckApiApplicationTests {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @Autowired private ApplicationContext context;

  @Test
  void contextLoads() {}

  @Test
  void noInMemoryUserIsCreated() {
    // OPS-10: no generated "user" account, and so no generated password in the logs.
    assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
  }

  @Test
  void main_shouldStartApplication() {
    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      String[] args = {};
      BrewdeckApiApplication.main(args);

      springApplication.verify(() -> SpringApplication.run(BrewdeckApiApplication.class, args));
    }
  }
}
