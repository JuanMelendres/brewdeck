package com.brewdeck.brewdeck_api.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = DashboardController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
              com.brewdeck.brewdeck_api.auth.JwtAuthenticationFilter.class,
              com.brewdeck.brewdeck_api.common.config.SecurityConfig.class,
              com.brewdeck.brewdeck_api.common.config.RestAuthenticationEntryPoint.class
            }))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class DashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DashboardService dashboardService;

  @Test
  void getSummary_shouldReturnSummary() throws Exception {
    when(dashboardService.getSummary())
        .thenReturn(new DashboardSummaryResponse(5L, 4L, 10L, 3L, 20L, 4.25));

    mockMvc
        .perform(get("/api/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCoffees").value(5))
        .andExpect(jsonPath("$.totalBrewMethods").value(4))
        .andExpect(jsonPath("$.totalRecipes").value(10))
        .andExpect(jsonPath("$.favoriteRecipes").value(3))
        .andExpect(jsonPath("$.totalBrewSessions").value(20))
        .andExpect(jsonPath("$.averageSessionRating").value(4.25));
  }

  @Test
  void getSummary_shouldReturnNullAverage_whenNoRatings() throws Exception {
    when(dashboardService.getSummary())
        .thenReturn(new DashboardSummaryResponse(0L, 0L, 0L, 0L, 0L, null));

    mockMvc
        .perform(get("/api/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalBrewSessions").value(0))
        .andExpect(jsonPath("$.averageSessionRating").doesNotExist());
  }
}
