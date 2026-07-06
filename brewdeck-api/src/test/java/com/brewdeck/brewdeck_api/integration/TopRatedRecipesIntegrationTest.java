package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class TopRatedRecipesIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void getTopRated_shouldRankRecipesByAverageRating() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long highRecipe = createRecipe(coffeeId, methodId, "High Rated");
    Long lowRecipe = createRecipe(coffeeId, methodId, "Low Rated");

    createBrewSession(highRecipe, 10);
    createBrewSession(highRecipe, 10);
    createBrewSession(lowRecipe, 4);

    String response =
        mockMvc
            .perform(get("/api/recipes/top-rated").param("limit", "20"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode ranking = objectMapper.readTree(response);
    assertThat(ranking.isArray()).isTrue();

    int highIndex = indexOfRecipe(ranking, highRecipe);
    int lowIndex = indexOfRecipe(ranking, lowRecipe);

    assertThat(highIndex).isGreaterThanOrEqualTo(0);
    assertThat(lowIndex).isGreaterThanOrEqualTo(0);
    assertThat(highIndex).isLessThan(lowIndex);
    assertThat(ranking.get(highIndex).get("averageRating").asDouble()).isEqualTo(10.0);
    assertThat(ranking.get(highIndex).get("totalSessions").asLong()).isEqualTo(2);
    assertThat(ranking.get(lowIndex).get("averageRating").asDouble()).isEqualTo(4.0);
  }

  @Test
  void getTopRated_shouldRespectLimit() throws Exception {
    String response =
        mockMvc
            .perform(get("/api/recipes/top-rated").param("limit", "1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode ranking = objectMapper.readTree(response);
    assertThat(ranking.size()).isLessThanOrEqualTo(1);
  }

  private int indexOfRecipe(JsonNode ranking, Long recipeId) {
    for (int i = 0; i < ranking.size(); i++) {
      if (ranking.get(i).get("recipeId").asLong() == recipeId) {
        return i;
      }
    }
    return -1;
  }

  private Long createCoffee() throws Exception {
    String body = "{\"name\": \"Top Coffee\"}";
    String response =
        mockMvc
            .perform(post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private Long createBrewMethod() throws Exception {
    String body =
        """
        { "name": "Top Method %d", "description": "Method for top-rated tests." }
        """
            .formatted(System.nanoTime());
    String response =
        mockMvc
            .perform(
                post("/api/brew-methods").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private Long createRecipe(Long coffeeId, Long methodId, String name) throws Exception {
    String body =
        """
        {
          "coffeeId": %d,
          "methodId": %d,
          "name": "%s",
          "coffeeGrams": 15,
          "waterGrams": 230,
          "waterTemp": 90,
          "favorite": false
        }
        """
            .formatted(coffeeId, methodId, name);
    String response =
        mockMvc
            .perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private void createBrewSession(Long recipeId, int rating) throws Exception {
    String body =
        """
        { "recipeId": %d, "actualTemp": 90, "actualTime": "2:30", "rating": %d }
        """
            .formatted(recipeId, rating);
    mockMvc
        .perform(post("/api/brew-sessions").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }
}
