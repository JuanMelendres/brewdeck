package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end public sharing: the owner shares, anyone can read a curated read-only view without
 * logging in, and unsharing (or re-sharing) kills the old link.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeShareFlowIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private RecipeRepository recipeRepository;

  @Test
  @WithMockUser
  void shareReadUnshareAndReshare() throws Exception {
    Long recipeId = ownRecipe().getId();

    String token = share(recipeId);
    assertThat(token).isNotBlank();
    // Sharing again is idempotent while the link is active.
    assertThat(share(recipeId)).isEqualTo(token);

    readPubliclyExpectingOk(token);

    mockMvc
        .perform(patch("/api/recipes/{id}/unshare", recipeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shareToken").doesNotExist());
    readPubliclyExpecting404(token);

    String newToken = share(recipeId);
    assertThat(newToken).isNotEqualTo(token);
    readPubliclyExpecting404(token);
    readPubliclyExpectingOk(newToken);
  }

  @Test
  @WithAnonymousUser
  void publicRead_ofUnknownToken_isNotFound() throws Exception {
    mockMvc
        .perform(get("/api/public/recipes/{token}", "no-such-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithAnonymousUser
  void publicLink_isReadOnly() throws Exception {
    Recipe recipe = ownRecipe();
    recipe.setShareToken("readonly-" + System.nanoTime());
    recipeRepository.save(recipe);

    // No write verb exists on the public path; anonymous writes to the private API are 401.
    mockMvc
        .perform(
            put("/api/public/recipes/{token}", recipe.getShareToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hijacked\"}"))
        .andExpect(status().isMethodNotAllowed());
    mockMvc
        .perform(
            put("/api/recipes/{id}", recipe.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hijacked\"}"))
        .andExpect(status().isUnauthorized());

    assertThat(recipeRepository.findById(recipe.getId()).orElseThrow().getName())
        .isEqualTo("Shared Pour Over");
  }

  private String share(Long recipeId) throws Exception {
    String response =
        mockMvc
            .perform(patch("/api/recipes/{id}/share", recipeId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.shareToken");
  }

  /** Anonymous read: curated fields only -- no ids, owner, or the token itself. */
  private void readPubliclyExpectingOk(String token) throws Exception {
    mockMvc
        .perform(
            get("/api/public/recipes/{token}", token)
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Shared Pour Over"))
        .andExpect(jsonPath("$.coffeeName").value("Shared Coffee"))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.coffeeId").doesNotExist())
        .andExpect(jsonPath("$.shareToken").doesNotExist())
        .andExpect(jsonPath("$.favorite").doesNotExist());
  }

  private void readPubliclyExpecting404(String token) throws Exception {
    mockMvc
        .perform(
            get("/api/public/recipes/{token}", token)
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.anonymous()))
        .andExpect(status().isNotFound());
  }

  private Recipe ownRecipe() {
    Coffee coffee =
        coffeeRepository.save(Coffee.builder().name("Shared Coffee").owner(mockUser()).build());
    BrewMethod method =
        brewMethodRepository.save(
            BrewMethod.builder().name("Share Flow Method " + System.nanoTime()).build());
    return recipeRepository.save(
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name("Shared Pour Over")
            .coffeeGrams(new BigDecimal("15"))
            .favorite(true)
            .owner(mockUser())
            .build());
  }
}
