package com.brewdeck.brewdeck_api.recipe;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private RecipeService recipeService;

  @Test
  void findAll_shouldReturnRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.findAll()).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/recipes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].coffeeName").value("Mezcla Veracruz"))
        .andExpect(jsonPath("$[0].methodName").value("AeroPress"));

    verify(recipeService).findAll();
  }

  @Test
  void findById_shouldReturnRecipe() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.findById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/recipes/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz AeroPress"));

    verify(recipeService).findById(1L);
  }

  @Test
  void findFavorites_shouldReturnFavoriteRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.findFavorites()).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/recipes/favorites"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].favorite").value(true));

    verify(recipeService).findFavorites();
  }

  @Test
  void findByCoffeeId_shouldReturnRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.findByCoffeeId(1L)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/recipes/coffee/{coffeeId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].coffeeId").value(1L));

    verify(recipeService).findByCoffeeId(1L);
  }

  @Test
  void findByMethodId_shouldReturnRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.findByMethodId(1L)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/recipes/method/{methodId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].methodId").value(1L));

    verify(recipeService).findByMethodId(1L);
  }

  @Test
  void create_shouldReturnCreatedRecipe() throws Exception {
    RecipeRequest request = buildRecipeRequest();
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.create(any(RecipeRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz AeroPress"));

    verify(recipeService).create(any(RecipeRequest.class));
  }

  @Test
  void create_shouldReturnBadRequest_whenRequiredFieldsAreMissing() throws Exception {
    RecipeRequest request =
        new RecipeRequest(null, null, "", null, null, null, null, null, null, null, null, false);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_shouldReturnUpdatedRecipe() throws Exception {
    RecipeRequest request = buildRecipeRequest();
    RecipeResponse response = buildRecipeResponse();

    when(recipeService.update(eq(1L), any(RecipeRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/recipes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));

    verify(recipeService).update(eq(1L), any(RecipeRequest.class));
  }

  @Test
  void delete_shouldDeleteRecipe() throws Exception {
    doNothing().when(recipeService).delete(1L);

    mockMvc.perform(delete("/api/recipes/{id}", 1L)).andExpect(status().isOk());

    verify(recipeService).delete(1L);
  }

  private RecipeRequest buildRecipeRequest() {
    return new RecipeRequest(
        1L,
        1L,
        "Mezcla Veracruz AeroPress",
        BigDecimal.valueOf(15),
        BigDecimal.valueOf(230),
        "1:15",
        "Timemore S3 - 5.5",
        90,
        "2:30",
        "Bloom 30s, stir gently, press slowly.",
        "Clean, aromatic, spicy, balanced.",
        true);
  }

  private RecipeResponse buildRecipeResponse() {
    return new RecipeResponse(
        1L,
        1L,
        "Mezcla Veracruz",
        1L,
        "AeroPress",
        "Mezcla Veracruz AeroPress",
        BigDecimal.valueOf(15),
        BigDecimal.valueOf(230),
        "1:15",
        "Timemore S3 - 5.5",
        90,
        "2:30",
        "Bloom 30s, stir gently, press slowly.",
        "Clean, aromatic, spicy, balanced.",
        true,
        LocalDateTime.now(),
        null);
  }
}
