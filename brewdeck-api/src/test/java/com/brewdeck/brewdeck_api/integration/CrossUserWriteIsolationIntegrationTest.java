package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The write-side companion of {@link CrossUserIsolationIntegrationTest}: the mock user can never
 * modify, delete, reference, favorite, or share another user's coffees, recipes, or brew sessions.
 * Every attempt is a 404 (existence is not revealed) and the foreign row is left untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class CrossUserWriteIsolationIntegrationTest extends PostgresIntegrationTest {

  private static final String OTHER_USER_EMAIL = "other-writes@brewdeck.test";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private RecipeRepository recipeRepository;
  @Autowired private BrewSessionRepository brewSessionRepository;

  private BrewMethod sharedMethod;
  private Coffee foreignCoffee;
  private Recipe foreignRecipe;
  private BrewSession foreignSession;

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

    sharedMethod =
        brewMethodRepository.save(
            BrewMethod.builder().name("Cross-User Writes Method " + System.nanoTime()).build());
    foreignCoffee =
        coffeeRepository.save(
            Coffee.builder().name("Foreign Coffee").origin("Kenya").owner(otherUser).build());
    foreignRecipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(foreignCoffee)
                .method(sharedMethod)
                .name("Foreign Recipe")
                .favorite(false)
                .owner(otherUser)
                .build());
    foreignSession =
        brewSessionRepository.save(
            BrewSession.builder()
                .recipe(foreignRecipe)
                .owner(otherUser)
                .brewedAt(LocalDateTime.now())
                .rating(8)
                .build());
  }

  // --- Coffees ---

  @Test
  void updateForeignCoffee_isNotFound_andLeavesItUnchanged() throws Exception {
    mockMvc
        .perform(json(put("/api/coffees/{id}", foreignCoffee.getId()), "{\"name\":\"Hijacked\"}"))
        .andExpect(status().isNotFound());

    Coffee reloaded = coffeeRepository.findById(foreignCoffee.getId()).orElseThrow();
    assertThat(reloaded.getName()).isEqualTo("Foreign Coffee");
    assertThat(reloaded.getOrigin()).isEqualTo("Kenya");
  }

  @Test
  void deleteForeignCoffee_isNotFound_andKeepsIt() throws Exception {
    mockMvc
        .perform(delete("/api/coffees/{id}", foreignCoffee.getId()))
        .andExpect(status().isNotFound());

    assertThat(coffeeRepository.existsById(foreignCoffee.getId())).isTrue();
  }

  // --- Recipes ---

  @Test
  void updateForeignRecipe_isNotFound_andLeavesItUnchanged() throws Exception {
    Coffee ownCoffee = ownCoffee();

    mockMvc
        .perform(
            json(
                put("/api/recipes/{id}", foreignRecipe.getId()),
                recipeBody(ownCoffee.getId(), "Hijacked")))
        .andExpect(status().isNotFound());

    assertThat(recipeRepository.findById(foreignRecipe.getId()).orElseThrow().getName())
        .isEqualTo("Foreign Recipe");
  }

  @Test
  void deleteForeignRecipe_isNotFound_andKeepsIt() throws Exception {
    mockMvc
        .perform(delete("/api/recipes/{id}", foreignRecipe.getId()))
        .andExpect(status().isNotFound());

    assertThat(recipeRepository.existsById(foreignRecipe.getId())).isTrue();
  }

  @Test
  void favoriteOrUnfavoriteForeignRecipe_isNotFound() throws Exception {
    mockMvc
        .perform(patch("/api/recipes/{id}/favorite", foreignRecipe.getId()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(patch("/api/recipes/{id}/unfavorite", foreignRecipe.getId()))
        .andExpect(status().isNotFound());

    assertThat(recipeRepository.findById(foreignRecipe.getId()).orElseThrow().getFavorite())
        .isFalse();
  }

  @Test
  void shareForeignRecipe_isNotFound_andCreatesNoPublicLink() throws Exception {
    mockMvc
        .perform(patch("/api/recipes/{id}/share", foreignRecipe.getId()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(patch("/api/recipes/{id}/unshare", foreignRecipe.getId()))
        .andExpect(status().isNotFound());

    assertThat(recipeRepository.findById(foreignRecipe.getId()).orElseThrow().getShareToken())
        .isNull();
  }

  @Test
  void foreignRecipeStats_isNotFound() throws Exception {
    mockMvc
        .perform(get("/api/recipes/{id}/stats", foreignRecipe.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void createRecipeWithForeignCoffee_isNotFound_andCreatesNothing() throws Exception {
    long before = recipeRepository.countByOwnerId(mockUser().getId());

    mockMvc
        .perform(json(post("/api/recipes"), recipeBody(foreignCoffee.getId(), "Borrowed Coffee")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Coffee not found"));

    assertThat(recipeRepository.countByOwnerId(mockUser().getId())).isEqualTo(before);
  }

  @Test
  void updateOwnRecipeToPointAtForeignCoffee_isNotFound() throws Exception {
    Coffee ownCoffee = ownCoffee();
    Recipe ownRecipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(ownCoffee)
                .method(sharedMethod)
                .name("Own Recipe")
                .favorite(false)
                .owner(mockUser())
                .build());

    mockMvc
        .perform(
            json(
                put("/api/recipes/{id}", ownRecipe.getId()),
                recipeBody(foreignCoffee.getId(), "Own Recipe")))
        .andExpect(status().isNotFound());

    assertThat(recipeRepository.findById(ownRecipe.getId()).orElseThrow().getCoffee().getId())
        .isEqualTo(ownCoffee.getId());
  }

  // --- Brew sessions ---

  @Test
  void createSessionOnForeignRecipe_isNotFound_andCreatesNothing() throws Exception {
    long before = brewSessionRepository.countByOwnerId(mockUser().getId());

    mockMvc
        .perform(
            json(
                post("/api/brew-sessions"),
                "{\"recipeId\":" + foreignRecipe.getId() + ",\"rating\":10}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Recipe not found"));

    assertThat(brewSessionRepository.countByOwnerId(mockUser().getId())).isEqualTo(before);
  }

  @Test
  void updateForeignSession_isNotFound_andLeavesItUnchanged() throws Exception {
    Recipe ownRecipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(ownCoffee())
                .method(sharedMethod)
                .name("Own Recipe For Session")
                .favorite(false)
                .owner(mockUser())
                .build());

    mockMvc
        .perform(
            json(
                put("/api/brew-sessions/{id}", foreignSession.getId()),
                "{\"recipeId\":" + ownRecipe.getId() + ",\"rating\":1}"))
        .andExpect(status().isNotFound());

    assertThat(brewSessionRepository.findById(foreignSession.getId()).orElseThrow().getRating())
        .isEqualTo(8);
  }

  @Test
  void deleteForeignSession_isNotFound_andKeepsIt() throws Exception {
    mockMvc
        .perform(delete("/api/brew-sessions/{id}", foreignSession.getId()))
        .andExpect(status().isNotFound());

    assertThat(brewSessionRepository.existsById(foreignSession.getId())).isTrue();
  }

  @Test
  void sessionsOfForeignRecipe_areNotListed() throws Exception {
    mockMvc
        .perform(
            get("/api/brew-sessions/recipe/{recipeId}", foreignRecipe.getId())
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  private Coffee ownCoffee() {
    return coffeeRepository.save(Coffee.builder().name("Own Coffee").owner(mockUser()).build());
  }

  private String recipeBody(Long coffeeId, String name) {
    return "{\"coffeeId\":"
        + coffeeId
        + ",\"methodId\":"
        + sharedMethod.getId()
        + ",\"name\":\""
        + name
        + "\"}";
  }

  private static MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder builder, String body) {
    return builder.contentType(MediaType.APPLICATION_JSON).content(body);
  }
}
