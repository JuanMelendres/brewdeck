package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.EnvironmentResolver;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Boots the hosted {@code staging} profile (ADR-017). The DB_* values only satisfy the profile's
 * required placeholders; the Testcontainers service connection supplies the real database.
 */
@SpringBootTest(
    properties = {
      "DB_URL=jdbc:postgresql://unused:5432/unused",
      "DB_USER=unused",
      "DB_PASSWORD=unused",
      "BREWDECK_JWT_SECRET=staging-profile-test-secret-0123456789abcdef"
    })
@AutoConfigureMockMvc
@ActiveProfiles("staging")
class StagingProfileIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EnvironmentResolver environmentResolver;
  @Autowired private DataSource dataSource;

  @Test
  void featureFlags_shouldResolveToStagingEnvironment() {
    assertThat(environmentResolver.resolve()).isEqualTo("staging");
  }

  @Test
  void connectionPool_shouldLetTheDatabaseScaleToZero() {
    HikariDataSource hikari = (HikariDataSource) dataSource;

    assertThat(hikari.getMinimumIdle()).isZero();
    assertThat(hikari.getMaximumPoolSize()).isEqualTo(5);
  }

  @Test
  void healthEndpoint_shouldReportUp() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void apiDocs_shouldBeDisabled() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
  }
}
