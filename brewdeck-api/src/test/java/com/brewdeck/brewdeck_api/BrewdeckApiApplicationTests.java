package com.brewdeck.brewdeck_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;
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
    // The service is not the retention job (ADR-021), so main returns instead of exiting.
    ConfigurableApplicationContext started = mock(ConfigurableApplicationContext.class);
    when(started.getEnvironment()).thenReturn(new MockEnvironment());
    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      String[] args = {};
      springApplication
          .when(() -> SpringApplication.run(BrewdeckApiApplication.class, args))
          .thenReturn(started);

      BrewdeckApiApplication.main(args);

      springApplication.verify(() -> SpringApplication.run(BrewdeckApiApplication.class, args));
      springApplication.verify(() -> SpringApplication.exit(started), never());
    }
  }
}
