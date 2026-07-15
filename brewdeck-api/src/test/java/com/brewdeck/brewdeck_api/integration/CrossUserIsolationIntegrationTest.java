package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that a user can never observe another user's coffees, recipes, or brew sessions through
 * any read endpoint, even though brew methods remain a shared/global resource whose usage counts
 * are still per-user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class CrossUserIsolationIntegrationTest extends PostgresIntegrationTest {

  private static final String OTHER_USER_EMAIL = "other@brewdeck.test";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private RecipeRepository recipeRepository;
  @Autowired private BrewSessionRepository brewSessionRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;

  private Long foreignCoffeeId;
  private Long foreignRecipeId;
  private Long foreignOnlyMethodId;

  @BeforeEach
  void seedForeignOwnedData() {
    User otherUser =
        userRepository
            .findByEmail(OTHER_USER_EMAIL)
            .orElseGet(
                () ->
                    userRepository.save(
                        User.builder()
                            .email(OTHER_USER_EMAIL)
                            .passwordHash("integration-test-placeholder")
                            .createdAt(LocalDateTime.now())
                            .build()));

    // A brand-new, uniquely-named method used ONLY by the foreign recipe below, so its usage
    // count is unambiguous: no other test in this shared Testcontainers database could have
    // created a recipe against it.
    BrewMethod method =
        brewMethodRepository.save(
            BrewMethod.builder()
                .name("Cross-User Isolation Method " + System.nanoTime())
                .description("Method created for cross-user isolation tests.")
                .build());
    foreignOnlyMethodId = method.getId();

    Coffee foreignCoffee =
        coffeeRepository.save(Coffee.builder().name("Foreign Coffee").owner(otherUser).build());
    foreignCoffeeId = foreignCoffee.getId();

    Recipe foreignRecipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(foreignCoffee)
                .method(method)
                .name("Foreign Recipe")
                .favorite(false)
                .owner(otherUser)
                .build());
    foreignRecipeId = foreignRecipe.getId();

    brewSessionRepository.save(
        BrewSession.builder()
            .recipe(foreignRecipe)
            .owner(otherUser)
            .brewedAt(LocalDateTime.now())
            .rating(10)
            .build());
  }

  @Test
  void getCoffees_shouldNotContainForeignCoffee() throws Exception {
    String response =
        mockMvc
            .perform(
                get("/api/coffees").param("page", "0").param("size", "200").param("sort", "id,asc"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode content = objectMapper.readTree(response).get("content");
    assertThat(content.isArray()).isTrue();
    for (JsonNode coffee : content) {
      assertThat(coffee.get("id").asLong()).isNotEqualTo(foreignCoffeeId);
    }
  }

  @Test
  void getCoffeeById_shouldReturnNotFound_forForeignCoffee() throws Exception {
    mockMvc.perform(get("/api/coffees/{id}", foreignCoffeeId)).andExpect(status().isNotFound());
  }

  @Test
  void getTopRatedRecipes_shouldNotIncludeForeignRecipe() throws Exception {
    String response =
        mockMvc
            .perform(get("/api/recipes/top-rated").param("limit", "20"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode ranking = objectMapper.readTree(response);
    assertThat(ranking.isArray()).isTrue();
    for (JsonNode recipe : ranking) {
      assertThat(recipe.get("recipeId").asLong()).isNotEqualTo(foreignRecipeId);
    }
  }

  @Test
  void getMethodUsage_shouldListSharedMethods_withZeroCountForForeignOnlyMethod() throws Exception {
    // Brew methods are shared/global, so the usage listing must still contain the method used
    // by the foreign recipe. But since that method was created fresh in @BeforeEach and used
    // ONLY by the foreign recipe (owned by the other user), its recipeCount for THIS user must
    // be exactly 0 -- proving the findUsage owner JOIN excludes the other user's recipe.
    long methodCount = brewMethodRepository.count();

    String response =
        mockMvc
            .perform(get("/api/brew-methods/usage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize((int) methodCount)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode usage = objectMapper.readTree(response);
    boolean foundForeignOnlyMethod = false;
    for (JsonNode entry : usage) {
      if (entry.get("methodId").asLong() == foreignOnlyMethodId) {
        foundForeignOnlyMethod = true;
        assertThat(entry.get("recipeCount").asLong()).isZero();
      }
    }
    assertThat(foundForeignOnlyMethod).isTrue();
  }

  @Test
  void getDashboardSummary_shouldExcludeForeignRows() throws Exception {
    // The @BeforeEach only ever creates rows for the OTHER user, so the authenticated mock
    // user's dashboard counts must exactly match what the repositories report for their own
    // owner id, independent of the foreign rows and of any pollution from other test classes
    // sharing this Testcontainers database.
    Long mockUserId = mockUser().getId();
    long expectedCoffees = coffeeRepository.countByOwnerId(mockUserId);
    long expectedRecipes = recipeRepository.countByOwnerId(mockUserId);
    long expectedSessions = brewSessionRepository.countByOwnerId(mockUserId);

    mockMvc
        .perform(get("/api/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCoffees", equalTo((int) expectedCoffees)))
        .andExpect(jsonPath("$.totalRecipes", equalTo((int) expectedRecipes)))
        .andExpect(jsonPath("$.totalBrewSessions", equalTo((int) expectedSessions)));
  }
}
