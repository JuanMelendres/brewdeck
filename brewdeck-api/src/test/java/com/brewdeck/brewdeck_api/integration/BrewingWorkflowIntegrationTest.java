package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
            .perform(
                get("/api/brew-sessions/recipe/{recipeId}", recipeId)
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode page = objectMapper.readTree(response);
    JsonNode sessions = page.get("content");

    assertThat(sessions).hasSize(1);
    assertThat(sessions.get(0).get("id").asLong()).isEqualTo(sessionId);
    assertThat(sessions.get(0).get("recipeId").asLong()).isEqualTo(recipeId);
    assertThat(sessions.get(0).get("recipeName").asText()).isEqualTo("Mezcla Veracruz AeroPress");
    assertThat(sessions.get(0).get("rating").asInt()).isEqualTo(9);
    assertThat(page.get("page").asInt()).isZero();
    assertThat(page.get("size").asInt()).isEqualTo(10);
    assertThat(page.get("totalElements").asLong()).isEqualTo(1);
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
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.coffeeId").value("Coffee id is required"))
        .andExpect(jsonPath("$.validationErrors.methodId").value("Brew method id is required"))
        .andExpect(jsonPath("$.validationErrors.name").value("Recipe name is required"));
  }

  @Test
  void createCoffee_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
    String requestBody =
        """
        {
          "name": ""
        }
        """;

    mockMvc
        .perform(post("/api/coffees").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Coffee name is required"));
  }

  @Test
  void createBrewMethod_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
    String requestBody =
        """
        {
          "name": ""
        }
        """;

    mockMvc
        .perform(
            post("/api/brew-methods").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Method name is required"));
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
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.rating").value("Rating must not exceed 10"));
  }

  @Test
  void searchCoffees_shouldReturnFilteredCoffees() throws Exception {
    createCoffee();

    mockMvc
        .perform(
            get("/api/coffees")
                .param("name", "Veracruz")
                .param("origin", "Veracruz")
                .param("roastLevel", "Medio")
                .param("process", "Lavado")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Mezcla Veracruz"))
        .andExpect(jsonPath("$.content[0].origin").value("Veracruz"))
        .andExpect(jsonPath("$.content[0].roastLevel").value("Medio"))
        .andExpect(jsonPath("$.content[0].process").value("Lavado"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void searchRecipes_shouldReturnFilteredRecipes() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    createRecipe(coffeeId, methodId);

    mockMvc
        .perform(
            get("/api/recipes")
                .param("coffeeId", coffeeId.toString())
                .param("methodId", methodId.toString())
                .param("favorite", "true")
                .param("name", "AeroPress")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].coffeeId").value(coffeeId))
        .andExpect(jsonPath("$.content[0].methodId").value(methodId))
        .andExpect(jsonPath("$.content[0].favorite").value(true))
        .andExpect(jsonPath("$.content[0].name").value("Mezcla Veracruz AeroPress"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void searchBrewSessions_shouldReturnFilteredSessions() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);
    createBrewSession(recipeId);

    mockMvc
        .perform(
            get("/api/brew-sessions")
                .param("recipeId", recipeId.toString())
                .param("rating", "9")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].recipeId").value(recipeId))
        .andExpect(jsonPath("$.content[0].rating").value(9))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void favoriteWorkflow_shouldMarkAndUnmarkRecipeAsFavorite() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);

    mockMvc
        .perform(patch("/api/recipes/{id}/favorite", recipeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(recipeId))
        .andExpect(jsonPath("$.favorite").value(true));

    String favoritesResponse =
        mockMvc
            .perform(
                get("/api/recipes/favorites")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode favoritesPage = objectMapper.readTree(favoritesResponse);
    JsonNode favorites = favoritesPage.get("content");

    assertThat(favorites).isNotEmpty();
    assertThat(favorites.toString()).contains("\"id\":" + recipeId);
    assertThat(favorites.toString()).contains("Mezcla Veracruz AeroPress");

    mockMvc
        .perform(patch("/api/recipes/{id}/unfavorite", recipeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(recipeId))
        .andExpect(jsonPath("$.favorite").value(false));
  }

  @Test
  void searchBrewSessions_shouldRespectPaginationSize() throws Exception {
    Long coffeeId = createCoffee();
    Long methodId = createBrewMethod();
    Long recipeId = createRecipe(coffeeId, methodId);

    createBrewSession(recipeId);
    createBrewSession(recipeId);

    mockMvc
        .perform(
            get("/api/brew-sessions")
                .param("recipeId", recipeId.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
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
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);

    assertThat(json.get("id").asLong()).isPositive();
    assertThat(json.get("name").asText()).isEqualTo("Mezcla Veracruz");

    return json.get("id").asLong();
  }

  private Long createBrewMethod() throws Exception {
    String methodName = "AeroPress " + System.nanoTime();

    String requestBody =
        """
            {
              "name": "%s",
              "description": "Immersion and pressure-based brewing method."
            }
            """
            .formatted(methodName);

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

    JsonNode json = objectMapper.readTree(response);

    assertThat(json.get("id").asLong()).isPositive();
    assertThat(json.get("name").asText()).startsWith("AeroPress");

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
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(response);

    Long recipeId = json.get("id").asLong();

    assertThat(recipeId).isPositive();
    assertThat(json.get("coffeeId").asLong()).isEqualTo(coffeeId);
    assertThat(json.get("methodId").asLong()).isEqualTo(methodId);
    assertThat(json.get("favorite").asBoolean()).isTrue();

    return recipeId;
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
            .andExpect(status().isCreated())
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
