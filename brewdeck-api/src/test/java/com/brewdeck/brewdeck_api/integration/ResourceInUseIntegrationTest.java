package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Deleting something other rows still use is a 409 that says what is in the way, never the generic
 * "Data integrity violation" from the foreign key. Once the dependents are gone, the delete works.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class ResourceInUseIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private RecipeRepository recipeRepository;
  @Autowired private BrewSessionRepository brewSessionRepository;

  @Test
  void coffeeUsedByRecipes_cannotBeDeleted_untilTheyAreGone() throws Exception {
    Coffee coffee = coffee();
    Recipe first = recipe(coffee, sharedMethod());
    Recipe second = recipe(coffee, sharedMethod());

    mockMvc
        .perform(delete("/api/coffees/{id}", coffee.getId()))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("Coffee is used by 2 recipes. Delete or change them first."));
    assertThat(coffeeRepository.existsById(coffee.getId())).isTrue();

    // Once the recipes are gone, the same delete succeeds.
    recipeRepository.deleteAllById(java.util.List.of(first.getId(), second.getId()));

    mockMvc.perform(delete("/api/coffees/{id}", coffee.getId())).andExpect(status().isNoContent());
  }

  @Test
  void recipeWithBrewSessions_cannotBeDeleted() throws Exception {
    Recipe recipe = recipe(coffee(), sharedMethod());
    brewSessionRepository.save(
        BrewSession.builder()
            .recipe(recipe)
            .owner(mockUser())
            .brewedAt(Instant.now())
            .rating(7)
            .build());

    mockMvc
        .perform(delete("/api/recipes/{id}", recipe.getId()))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("Recipe is used by 1 brew session. Delete or change it first."));
    assertThat(recipeRepository.existsById(recipe.getId())).isTrue();
  }

  @Test
  void privateMethodUsedByARecipe_cannotBeDeleted() throws Exception {
    BrewMethod mine =
        brewMethodRepository.save(
            BrewMethod.builder()
                .name("In-use private " + System.nanoTime())
                .owner(mockUser())
                .build());
    recipe(coffee(), mine);

    mockMvc
        .perform(delete("/api/brew-methods/{id}", mine.getId()))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("Brew method is used by 1 recipe. Delete or change it first."));
    assertThat(brewMethodRepository.existsById(mine.getId())).isTrue();
  }

  @Test
  void unusedCoffee_isDeleted() throws Exception {
    Coffee coffee = coffee();

    mockMvc.perform(delete("/api/coffees/{id}", coffee.getId())).andExpect(status().isNoContent());
    assertThat(coffeeRepository.existsById(coffee.getId())).isFalse();
  }

  private Coffee coffee() {
    return coffeeRepository.save(Coffee.builder().name("In-use coffee").owner(mockUser()).build());
  }

  private BrewMethod sharedMethod() {
    return brewMethodRepository.save(
        BrewMethod.builder().name("In-use shared " + System.nanoTime()).build());
  }

  private Recipe recipe(Coffee coffee, BrewMethod method) {
    return recipeRepository.save(
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("In-use recipe")
            .favorite(false)
            .owner(mockUser())
            .build());
  }
}
