package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeStatsIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void getStats_shouldAggregateSessionsForRecipe() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);
    createBrewSession(recipeId);
    createBrewSession(recipeId);

    mockMvc
        .perform(get("/api/recipes/{id}/stats", recipeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipeId").value(recipeId))
        .andExpect(jsonPath("$.totalSessions").value(2))
        .andExpect(jsonPath("$.averageRating").value(9.0))
        .andExpect(jsonPath("$.lastBrewedAt").exists());
  }

  @Test
  void getStats_shouldReturnZeros_whenRecipeHasNoSessions() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);

    mockMvc
        .perform(get("/api/recipes/{id}/stats", recipeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipeId").value(recipeId))
        .andExpect(jsonPath("$.totalSessions").value(0))
        .andExpect(jsonPath("$.averageRating").doesNotExist())
        .andExpect(jsonPath("$.lastBrewedAt").doesNotExist());
  }

  @Test
  void getStats_shouldReturnNotFound_whenRecipeDoesNotExist() throws Exception {
    mockMvc
        .perform(get("/api/recipes/{id}/stats", 999_999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Recipe not found"));
  }

  @Test
  void shareFlow_createShareFetchUnshareRefetch() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);

    String token =
        JsonPath.read(
            mockMvc
                .perform(patch("/api/recipes/" + recipeId + "/share"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.shareToken");

    mockMvc
        .perform(get("/api/public/recipes/" + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").exists());

    mockMvc.perform(patch("/api/recipes/" + recipeId + "/unshare")).andExpect(status().isOk());

    mockMvc.perform(get("/api/public/recipes/" + token)).andExpect(status().isNotFound());
  }

  private Long createCoffee() throws Exception {
    String requestBody =
        """
        {
          "name": "Stats Coffee",
          "origin": "Veracruz",
          "roastLevel": "Medio",
          "process": "Lavado"
        }
        """;

    String response =
        mockMvc
            .perform(
                post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }

  private Long createBrewMethod() throws Exception {
    String requestBody =
        """
        {
          "name": "Stats Method %d",
          "description": "Immersion and pressure-based brewing method."
        }
        """
            .formatted(System.nanoTime());

    String response =
        mockMvc
            .perform(
                post("/api/brew-methods")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }

  private Long createRecipe(Long coffeeId, Long methodId) throws Exception {
    String requestBody =
        """
        {
          "coffeeId": %d,
          "methodId": %d,
          "name": "Stats Recipe",
          "coffeeGrams": 15,
          "waterGrams": 230,
          "ratio": "1:15",
          "grindSetting": "Timemore S3 - 5.5",
          "waterTemp": 90,
          "brewTime": "2:30",
          "steps": "Bloom 30s, stir gently, press slowly.",
          "expectedTaste": "Clean, aromatic, spicy, balanced.",
          "favorite": false
        }
        """
            .formatted(coffeeId, methodId);

    String response =
        mockMvc
            .perform(
                post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response).get("id").asLong();
  }

  private void createBrewSession(Long recipeId) throws Exception {
    String requestBody =
        """
        {
          "recipeId": %d,
          "actualGrind": "Timemore S3 - 5.5",
          "actualTemp": 90,
          "actualTime": "2:30",
          "tasteResult": "Balanced, clean and aromatic.",
          "rating": 9,
          "adjustmentNotes": "Repeat same recipe next time."
        }
        """
            .formatted(recipeId);

    String response =
        mockMvc
            .perform(
                post("/api/brew-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);
    assertThat(json.get("id").asLong()).isPositive();
  }
}
