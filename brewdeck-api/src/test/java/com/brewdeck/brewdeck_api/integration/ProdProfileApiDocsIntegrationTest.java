package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Boots with the real {@code prod} profile and checks that the OpenAPI spec and Swagger UI are not
 * served (audit finding 7). {@link OpenApiDocsIntegrationTest} covers the non-prod case where they
 * are. The datasource placeholders are required by application-prod.yml but overridden by the
 * Testcontainers service connection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(
    properties = {
      "DB_URL=jdbc:postgresql://overridden-by-testcontainers/brewdeck",
      "DB_USER=overridden",
      "DB_PASSWORD=overridden",
      "BREWDECK_JWT_SECRET=prod-profile-test-secret-that-is-at-least-32-bytes-long"
    })
class ProdProfileApiDocsIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiSpec_isNotServedInProd() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
  }

  @Test
  void swaggerUi_isNotServedInProd() throws Exception {
    mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
  }

  @Test
  void healthCheck_isStillPublicInProd() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
