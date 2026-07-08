package com.brewdeck.brewdeck_api.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class DashboardSummaryIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void getSummary_shouldReturnAggregatedCounts() throws Exception {
    mockMvc
        .perform(get("/api/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCoffees", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.totalBrewMethods", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.totalRecipes", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.favoriteRecipes", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.totalBrewSessions", greaterThanOrEqualTo(0)));
  }
}
