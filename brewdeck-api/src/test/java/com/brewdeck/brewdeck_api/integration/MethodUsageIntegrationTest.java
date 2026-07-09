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
class MethodUsageIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void getUsage_shouldCountRecipesPerMethodAndIncludeUnusedMethods() throws Exception {
    Long coffeeId = createCoffee();
    Long usedMethod = createBrewMethod();
    Long unusedMethod = createBrewMethod();

    createRecipe(coffeeId, usedMethod, "Usage Recipe 1");
    createRecipe(coffeeId, usedMethod, "Usage Recipe 2");

    String response =
        mockMvc
            .perform(get("/api/brew-methods/usage"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode usage = objectMapper.readTree(response);
    assertThat(usage.isArray()).isTrue();

    int usedIndex = indexOfMethod(usage, usedMethod);
    int unusedIndex = indexOfMethod(usage, unusedMethod);

    assertThat(usedIndex).isGreaterThanOrEqualTo(0);
    assertThat(unusedIndex).isGreaterThanOrEqualTo(0);
    assertThat(usage.get(usedIndex).get("recipeCount").asLong()).isEqualTo(2);
    assertThat(usage.get(unusedIndex).get("recipeCount").asLong()).isZero();
    // ordered by recipe count desc: the used method must appear before the unused one
    assertThat(usedIndex).isLessThan(unusedIndex);
  }

  private int indexOfMethod(JsonNode usage, Long methodId) {
    for (int i = 0; i < usage.size(); i++) {
      if (usage.get(i).get("methodId").asLong() == methodId) {
        return i;
      }
    }
    return -1;
  }

  private Long createCoffee() throws Exception {
    String body = "{\"name\": \"Usage Coffee\"}";
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
        { "name": "Usage Method %d", "description": "Method for usage tests." }
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
