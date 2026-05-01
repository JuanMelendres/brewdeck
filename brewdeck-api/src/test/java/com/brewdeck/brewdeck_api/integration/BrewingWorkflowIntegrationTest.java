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
class BrewingWorkflowIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void completeBrewingWorkflow_shouldCreateCoffeeMethodRecipeAndBrewSession() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);
    Long sessionId = createBrewSession(recipeId);

    String response =
        mockMvc
            .perform(get("/api/brew-sessions/recipe/{recipeId}", recipeId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode sessions = objectMapper.readTree(response);

    assertThat(sessions).hasSize(1);
    assertThat(sessions.get(0).get("id").asLong()).isEqualTo(sessionId);
    assertThat(sessions.get(0).get("recipeId").asLong()).isEqualTo(recipeId);
    assertThat(sessions.get(0).get("recipeName").asText()).isEqualTo("Mezcla Veracruz AeroPress");
    assertThat(sessions.get(0).get("rating").asInt()).isEqualTo(9);
  }

  @Test
  void createRecipe_shouldReturnBadRequest_whenRequiredFieldsAreMissing() throws Exception {
    String requestBody =
        """
        {
          "coffeeId": null,
          "methodId": null,
          "name": ""
        }
        """;

    mockMvc
        .perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createBrewSession_shouldReturnBadRequest_whenRatingIsOutOfRange() throws Exception {
    String requestBody =
        """
        {
          "recipeId": 1,
          "actualGrind": "Timemore S3 - 5.5",
          "actualTemp": 90,
          "actualTime": "2:30",
          "tasteResult": "Balanced",
          "rating": 11,
          "adjustmentNotes": "Invalid rating"
        }
        """;

    mockMvc
        .perform(
            post("/api/brew-sessions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }

  private Long createCoffee() throws Exception {
    String requestBody =
        """
        {
          "name": "Mezcla Veracruz",
          "brand": "Café local",
          "origin": "Veracruz",
          "region": "Coatepec",
          "farm": null,
          "producer": null,
          "variety": "Blend",
          "process": "Lavado",
          "roastLevel": "Medio",
          "notesPrimary": "Cardamomo",
          "notesSecondary": "Canela, clavo, limpio, aromático",
          "acidity": "Media",
          "body": "Medio",
          "sweetness": "Media",
          "bitterness": "Baja",
          "description": "Café especiado, limpio y aromático, ideal para AeroPress."
        }
        """;

    String response =
        mockMvc
            .perform(
                post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);

    assertThat(json.get("id").asLong()).isPositive();
    assertThat(json.get("name").asText()).isEqualTo("Mezcla Veracruz");

    return json.get("id").asLong();
  }

  private Long createBrewMethod() throws Exception {
    String requestBody =
        """
        {
          "name": "AeroPress",
          "description": "Immersion and pressure-based brewing method."
        }
        """;

    String response =
        mockMvc
            .perform(
                post("/api/brew-methods")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);

    assertThat(json.get("id").asLong()).isPositive();
    assertThat(json.get("name").asText()).isEqualTo("AeroPress");

    return json.get("id").asLong();
  }

  private Long createRecipe(Long coffeeId, Long methodId) throws Exception {
    String requestBody =
        """
        {
          "coffeeId": %d,
          "methodId": %d,
          "name": "Mezcla Veracruz AeroPress",
          "coffeeGrams": 15,
          "waterGrams": 230,
          "ratio": "1:15",
          "grindSetting": "Timemore S3 - 5.5",
          "waterTemp": 90,
          "brewTime": "2:30",
          "steps": "Bloom 30s, stir gently, press slowly.",
          "expectedTaste": "Clean, aromatic, spicy, balanced.",
          "favorite": true
        }
        """
            .formatted(coffeeId, methodId);

    String response =
        mockMvc
            .perform(
                post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);

    assertThat(json.get("id").asLong()).isPositive();
    assertThat(json.get("coffeeId").asLong()).isEqualTo(coffeeId);
    assertThat(json.get("methodId").asLong()).isEqualTo(methodId);
    assertThat(json.get("favorite").asBoolean()).isTrue();

    return json.get("id").asLong();
  }

  private Long createBrewSession(Long recipeId) throws Exception {
    String requestBody =
        """
        {
          "recipeId": %d,
          "actualGrind": "Timemore S3 - 5.5",
          "actualTemp": 90,
          "actualTime": "2:30",
          "tasteResult": "Balanced, clean and aromatic. Cardamom and cinnamon notes were noticeable.",
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
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);

    assertThat(json.get("id").asLong()).isPositive();
    assertThat(json.get("recipeId").asLong()).isEqualTo(recipeId);
    assertThat(json.get("rating").asInt()).isEqualTo(9);

    return json.get("id").asLong();
  }
}
