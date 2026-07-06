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
class MostUsedCoffeesIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void getMostUsed_shouldRankCoffeesByRecipeCount() throws Exception {
    Long methodId = createBrewMethod();
    Long popularCoffee = createCoffee("Popular Coffee");
    Long rareCoffee = createCoffee("Rare Coffee");

    createRecipe(popularCoffee, methodId, "Popular Recipe 1");
    createRecipe(popularCoffee, methodId, "Popular Recipe 2");
    createRecipe(rareCoffee, methodId, "Rare Recipe 1");

    String response =
        mockMvc
            .perform(get("/api/coffees/most-used").param("limit", "20"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode ranking = objectMapper.readTree(response);
    assertThat(ranking.isArray()).isTrue();

    int popularIndex = indexOfCoffee(ranking, popularCoffee);
    int rareIndex = indexOfCoffee(ranking, rareCoffee);

    assertThat(popularIndex).isGreaterThanOrEqualTo(0);
    assertThat(rareIndex).isGreaterThanOrEqualTo(0);
    assertThat(popularIndex).isLessThan(rareIndex);
    assertThat(ranking.get(popularIndex).get("recipeCount").asLong()).isEqualTo(2);
    assertThat(ranking.get(rareIndex).get("recipeCount").asLong()).isEqualTo(1);
  }

  @Test
  void getMostUsed_shouldRespectLimit() throws Exception {
    String response =
        mockMvc
            .perform(get("/api/coffees/most-used").param("limit", "1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode ranking = objectMapper.readTree(response);
    assertThat(ranking.size()).isLessThanOrEqualTo(1);
  }

  private int indexOfCoffee(JsonNode ranking, Long coffeeId) {
    for (int i = 0; i < ranking.size(); i++) {
      if (ranking.get(i).get("coffeeId").asLong() == coffeeId) {
        return i;
      }
    }
    return -1;
  }

  private Long createCoffee(String name) throws Exception {
    String body = "{\"name\": \"%s\"}".formatted(name);
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
        { "name": "Usage Method %d", "description": "Method for coffee usage tests." }
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

  private void createRecipe(Long coffeeId, Long methodId, String name) throws Exception {
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
    mockMvc
        .perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }
}
