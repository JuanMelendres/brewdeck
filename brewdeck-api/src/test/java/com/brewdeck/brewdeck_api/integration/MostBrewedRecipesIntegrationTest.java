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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class MostBrewedRecipesIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void getMostBrewed_shouldRankRecipesBySessionCount() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long busyRecipe = createRecipe(coffeeId, methodId, "Busy Recipe");
    Long quietRecipe = createRecipe(coffeeId, methodId, "Quiet Recipe");

    createBrewSession(busyRecipe);
    createBrewSession(busyRecipe);
    createBrewSession(busyRecipe);
    createBrewSession(quietRecipe);

    String response =
        mockMvc
            .perform(get("/api/recipes/most-brewed").param("limit", "20"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode ranking = objectMapper.readTree(response);
    assertThat(ranking.isArray()).isTrue();

    int busyIndex = indexOfRecipe(ranking, busyRecipe);
    int quietIndex = indexOfRecipe(ranking, quietRecipe);

    assertThat(busyIndex).isGreaterThanOrEqualTo(0);
    assertThat(quietIndex).isGreaterThanOrEqualTo(0);
    assertThat(busyIndex).isLessThan(quietIndex);
    assertThat(ranking.get(busyIndex).get("totalSessions").asLong()).isEqualTo(3);
    assertThat(ranking.get(quietIndex).get("totalSessions").asLong()).isEqualTo(1);
  }

  @Test
  void getMostBrewed_shouldRespectLimit() throws Exception {
    String response =
        mockMvc
            .perform(get("/api/recipes/most-brewed").param("limit", "1"))
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
    String body = "{\"name\": \"Brew Coffee\"}";
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
        { "name": "Brew Method %d", "description": "Method for most-brewed tests." }
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

  private void createBrewSession(Long recipeId) throws Exception {
    String body =
        """
        { "recipeId": %d, "actualTemp": 90, "actualTime": "2:30", "rating": 8 }
        """
            .formatted(recipeId);
    mockMvc
        .perform(post("/api/brew-sessions").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }
}
