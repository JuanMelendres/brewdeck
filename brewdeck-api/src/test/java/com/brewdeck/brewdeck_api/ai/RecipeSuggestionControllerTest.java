package com.brewdeck.brewdeck_api.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = RecipeSuggestionController.class,
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
class RecipeSuggestionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private RecipeSuggestionService service;

  @Test
  void suggest_shouldReturnSuggestion() throws Exception {
    when(service.suggest(any()))
        .thenReturn(
            new SuggestedRecipeResponse(
                new BigDecimal("15"),
                new BigDecimal("240"),
                "1:16",
                "Medium-fine",
                92,
                "2:30",
                "Bloom then pour.",
                "Balanced for a medium roast."));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":1,\"methodId\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.waterTemp").value(92))
        .andExpect(jsonPath("$.ratio").value("1:16"))
        .andExpect(jsonPath("$.rationale").value("Balanced for a medium roast."));
  }

  @Test
  void suggest_shouldReturnBadRequest_whenIdsMissing() throws Exception {
    mockMvc
        .perform(post("/api/recipes/suggest").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.coffeeId").value("Coffee id is required"))
        .andExpect(jsonPath("$.validationErrors.methodId").value("Brew method id is required"));
  }

  @Test
  void suggest_shouldReturnServiceUnavailable_whenAiUnavailable() throws Exception {
    when(service.suggest(any())).thenThrow(new AiUnavailableException("disabled"));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":1,\"methodId\":2}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void suggest_shouldReturnNotFound_whenEntityMissing() throws Exception {
    when(service.suggest(any())).thenThrow(new EntityNotFoundException("Coffee not found"));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":9,\"methodId\":2}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void suggest_shouldReturnNotFound_whenFeatureFlagDisabled() throws Exception {
    when(service.suggest(any()))
        .thenThrow(
            new com.brewdeck.brewdeck_api.featureflag.FeatureDisabledException(
                com.brewdeck.brewdeck_api.featureflag.FeatureKeys.AI_RECIPE_ASSISTANT,
                org.springframework.http.HttpStatus.NOT_FOUND));

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coffeeId\":1,\"methodId\":2}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
