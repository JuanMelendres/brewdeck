package com.brewdeck.brewdeck_api.ai;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeImprovementController.class)
class RecipeImprovementControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private RecipeImprovementService service;

  @Test
  void improve_shouldReturnImprovement() throws Exception {
    when(service.improve(5L))
        .thenReturn(
            new SuggestedRecipeResponse(
                new BigDecimal("16"),
                new BigDecimal("240"),
                "1:15",
                "Timemore S3 - 5.0",
                92,
                "2:15",
                "Grind finer.",
                "Finer grind improves sweetness."));

    mockMvc
        .perform(post("/api/recipes/5/improve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.waterTemp").value(92))
        .andExpect(jsonPath("$.rationale").value("Finer grind improves sweetness."));
  }

  @Test
  void improve_shouldReturnNotFound_whenRecipeMissing() throws Exception {
    when(service.improve(5L)).thenThrow(new EntityNotFoundException("Recipe not found"));

    mockMvc.perform(post("/api/recipes/5/improve")).andExpect(status().isNotFound());
  }

  @Test
  void improve_shouldReturnUnprocessable_whenNoRatedHistory() throws Exception {
    when(service.improve(5L))
        .thenThrow(new InsufficientBrewHistoryException("Recipe has no rated brew sessions"));

    mockMvc
        .perform(post("/api/recipes/5/improve"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));
  }

  @Test
  void improve_shouldReturnServiceUnavailable_whenAiUnavailable() throws Exception {
    when(service.improve(5L)).thenThrow(new AiUnavailableException("disabled"));

    mockMvc
        .perform(post("/api/recipes/5/improve"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }
}
