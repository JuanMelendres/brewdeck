package com.brewdeck.brewdeck_api.recipe;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicRecipeController.class)
class PublicRecipeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RecipeService recipeService;

  @Test
  void getByToken_returns200WithCuratedBodyAndNoInternalFields() throws Exception {
    PublicRecipeResponse dto =
        new PublicRecipeResponse(
            "Morning Cup",
            "Ethiopia",
            "V60",
            new BigDecimal("15.0"),
            new BigDecimal("250.0"),
            "1:16",
            "Medium",
            94,
            "3:00",
            "Bloom then pour",
            "Floral");
    when(recipeService.getByShareToken("tok-1")).thenReturn(dto);

    mockMvc
        .perform(get("/api/public/recipes/tok-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Morning Cup"))
        .andExpect(jsonPath("$.coffeeName").value("Ethiopia"))
        .andExpect(jsonPath("$.methodName").value("V60"))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.favorite").doesNotExist())
        .andExpect(jsonPath("$.createdAt").doesNotExist());
  }

  @Test
  void getByToken_returns404WhenTokenUnknown() throws Exception {
    when(recipeService.getByShareToken("nope"))
        .thenThrow(new EntityNotFoundException("Recipe not found"));

    mockMvc.perform(get("/api/public/recipes/nope")).andExpect(status().isNotFound());
  }
}
