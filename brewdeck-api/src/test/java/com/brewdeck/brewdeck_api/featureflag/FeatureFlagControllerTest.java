package com.brewdeck.brewdeck_api.featureflag;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.User;
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
    controllers = FeatureFlagController.class,
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
class FeatureFlagControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FeatureFlagService featureFlagService;
  @MockitoBean private CurrentUserProvider currentUserProvider;

  @Test
  void returnsAliasMap_forCurrentUser() throws Exception {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(featureFlagService.isEnabled(
            eq(FeatureKeys.AI_RECIPE_ASSISTANT), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    mockMvc
        .perform(get("/api/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.aiRecipeAssistant").value(true));
  }

  @Test
  void reportsDisabledFeaturesAsFalse() throws Exception {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(featureFlagService.isEnabled(
            eq(FeatureKeys.AI_RECIPE_ASSISTANT), org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    mockMvc
        .perform(get("/api/feature-flags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.aiRecipeAssistant").value(false));
  }
}
