package com.brewdeck.brewdeck_api.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.ai.RecipeSuggestionPort;
import com.brewdeck.brewdeck_api.ai.SuggestedRecipe;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagCache;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The AI assistant can never read another user's data into a prompt (AUTH-05.8). The release flag
 * is switched on and the model is a mock, so a 404 here comes from the owner-scoped lookup (its
 * message names the missing resource), not from the flag gate, and the mock proves nothing reached
 * the model. The own-data cases show the same setup does reach it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class CrossUserAiIsolationIntegrationTest extends PostgresIntegrationTest {

  private static final String OTHER_USER_EMAIL = "other-ai@brewdeck.test";
  private static final String TEST_ENV = "test";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private RecipeRepository recipeRepository;
  @Autowired private BrewSessionRepository brewSessionRepository;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;
  @Autowired private FeatureFlagCache featureFlagCache;

  @MockitoBean private RecipeSuggestionPort suggestionPort;

  private BrewMethod sharedMethod;
  private Coffee foreignCoffee;
  private Recipe foreignRecipe;
  private BrewMethod foreignPrivateMethod;

  @BeforeEach
  void enableAssistantAndSeedForeignData() {
    featureFlagAdminService.setEnabled(FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV, true, "test");

    User otherUser =
        userRepository
            .findByEmail(OTHER_USER_EMAIL)
            .orElseGet(
                () ->
                    userRepository.save(
                        User.builder()
                            .email(OTHER_USER_EMAIL)
                            .passwordHash("integration-test-placeholder")
                            .createdAt(Instant.now())
                            .build()));

    sharedMethod =
        brewMethodRepository.save(
            BrewMethod.builder().name("Cross-User AI Method " + System.nanoTime()).build());
    foreignPrivateMethod =
        brewMethodRepository.save(
            BrewMethod.builder()
                .name("Foreign Private Method " + System.nanoTime())
                .owner(otherUser)
                .build());
    foreignCoffee =
        coffeeRepository.save(Coffee.builder().name("Foreign AI Coffee").owner(otherUser).build());
    foreignRecipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(foreignCoffee)
                .method(sharedMethod)
                .name("Foreign AI Recipe")
                .favorite(false)
                .owner(otherUser)
                .build());
    // Rated history, so "no rated sessions" can never be the reason improve is refused.
    brewSessionRepository.save(
        BrewSession.builder()
            .recipe(foreignRecipe)
            .owner(otherUser)
            .brewedAt(Instant.now())
            .rating(9)
            .build());

    when(suggestionPort.suggest(any())).thenReturn(suggestion());
    when(suggestionPort.improve(any())).thenReturn(suggestion());
  }

  @AfterEach
  void restoreSeededFlag() {
    featureFlagAdminService.setEnabled(
        FeatureKeys.AI_RECIPE_ASSISTANT, TEST_ENV, false, "test-teardown");
    featureFlagCache.evictAll();
  }

  @Test
  void improve_shouldReturnNotFound_forForeignRecipe() throws Exception {
    mockMvc
        .perform(post("/api/recipes/{id}/improve", foreignRecipe.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Recipe not found"));

    verify(suggestionPort, never()).improve(any());
  }

  @Test
  void improve_shouldReachTheModel_forOwnRecipe() throws Exception {
    Recipe ownRecipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(ownCoffee())
                .method(sharedMethod)
                .name("Own AI Recipe")
                .favorite(false)
                .owner(mockUser())
                .build());
    brewSessionRepository.save(
        BrewSession.builder()
            .recipe(ownRecipe)
            .owner(mockUser())
            .brewedAt(Instant.now())
            .rating(7)
            .build());

    mockMvc
        .perform(post("/api/recipes/{id}/improve", ownRecipe.getId()))
        .andExpect(status().isOk());

    verify(suggestionPort).improve(any());
  }

  @Test
  void suggest_shouldReturnNotFound_forForeignCoffee() throws Exception {
    mockMvc
        .perform(suggestRequest(foreignCoffee.getId(), sharedMethod.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Coffee not found"));

    verify(suggestionPort, never()).suggest(any());
  }

  @Test
  void suggest_shouldReturnNotFound_forForeignPrivateMethod() throws Exception {
    mockMvc
        .perform(suggestRequest(ownCoffee().getId(), foreignPrivateMethod.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Brew method not found"));

    verify(suggestionPort, never()).suggest(any());
  }

  @Test
  void suggest_shouldReachTheModel_forOwnCoffeeAndSharedMethod() throws Exception {
    mockMvc
        .perform(suggestRequest(ownCoffee().getId(), sharedMethod.getId()))
        .andExpect(status().isOk());

    verify(suggestionPort).suggest(any());
  }

  private Coffee ownCoffee() {
    return coffeeRepository.save(Coffee.builder().name("Own AI Coffee").owner(mockUser()).build());
  }

  private static MockHttpServletRequestBuilder suggestRequest(Long coffeeId, Long methodId) {
    return post("/api/recipes/suggest")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"coffeeId\":" + coffeeId + ",\"methodId\":" + methodId + "}");
  }

  private static SuggestedRecipe suggestion() {
    return new SuggestedRecipe(
        new BigDecimal("15.00"),
        new BigDecimal("250.00"),
        "1:16.7",
        "Medium",
        new BigDecimal("93"),
        "3:00",
        "Bloom 30 s, then pour slowly.",
        "Balanced starting point.");
  }
}
