package com.brewdeck.brewdeck_api.recipe;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    RecipeFilter filter = new RecipeFilter(null, null, null, null);

    PageResponse<RecipeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(recipeService.search(eq(filter), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/recipes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.content[0].coffeeName").value("Mezcla Veracruz"))
        .andExpect(jsonPath("$.content[0].methodName").value("AeroPress"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(recipeService).search(eq(filter), any(Pageable.class));
  }

  @Test
  void findAll_shouldReturnFilteredRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();
    RecipeFilter filter = new RecipeFilter(1L, 1L, true, "AeroPress");

    PageResponse<RecipeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 5), 1));

    when(recipeService.search(eq(filter), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(
            get("/api/recipes")
                .param("coffeeId", "1")
                .param("methodId", "1")
                .param("favorite", "true")
                .param("name", "AeroPress")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "id,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(1L))
        .andExpect(jsonPath("$.content[0].coffeeId").value(1L))
        .andExpect(jsonPath("$.content[0].methodId").value(1L))
        .andExpect(jsonPath("$.content[0].favorite").value(true))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5));

    verify(recipeService).search(eq(filter), any(Pageable.class));
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

    PageResponse<RecipeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(recipeService.findFavorites(any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/recipes/favorites"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].favorite").value(true))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(recipeService).findFavorites(any(Pageable.class));
  }

  @Test
  void findByCoffeeId_shouldReturnRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    PageResponse<RecipeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(recipeService.findByCoffeeId(eq(1L), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/recipes/coffee/{coffeeId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].coffeeId").value(1L))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(recipeService).findByCoffeeId(eq(1L), any(Pageable.class));
  }

  @Test
  void findByMethodId_shouldReturnRecipes() throws Exception {
    RecipeResponse response = buildRecipeResponse();

    PageResponse<RecipeResponse> pageResponse =
        PageResponse.fromPage(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

    when(recipeService.findByMethodId(eq(1L), any(Pageable.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/recipes/method/{methodId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].methodId").value(1L))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(recipeService).findByMethodId(eq(1L), any(Pageable.class));
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
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.name").value("Mezcla Veracruz AeroPress"));

    verify(recipeService).create(any(RecipeRequest.class));
  }

  @Test
  void create_shouldReturnValidationErrors_whenRequiredFieldsAreMissing() throws Exception {
    RecipeRequest request =
        new RecipeRequest(null, null, "", null, null, null, null, null, null, null, null, false);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.coffeeId").value("Coffee id is required"))
        .andExpect(jsonPath("$.validationErrors.methodId").value("Brew method id is required"))
        .andExpect(jsonPath("$.validationErrors.name").value("Recipe name is required"));
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

    mockMvc.perform(delete("/api/recipes/{id}", 1L)).andExpect(status().isNoContent());

    verify(recipeService).delete(1L);
  }

  @Test
  void markAsFavorite_shouldReturnRecipeAsFavorite() throws Exception {
    RecipeResponse response =
        new RecipeResponse(
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

    when(recipeService.markAsFavorite(1L)).thenReturn(response);

    mockMvc
        .perform(patch("/api/recipes/{id}/favorite", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.favorite").value(true));

    verify(recipeService).markAsFavorite(1L);
  }

  @Test
  void removeFromFavorites_shouldReturnRecipeAsNotFavorite() throws Exception {
    RecipeResponse response =
        new RecipeResponse(
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
            false,
            LocalDateTime.now(),
            null);

    when(recipeService.removeFromFavorites(1L)).thenReturn(response);

    mockMvc
        .perform(patch("/api/recipes/{id}/unfavorite", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.favorite").value(false));

    verify(recipeService).removeFromFavorites(1L);
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

  @Test
  void create_shouldReturnBadRequest_whenCoffeeGramsIsInvalid() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "Mezcla Veracruz AeroPress",
            BigDecimal.valueOf(-15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "Bloom 30s, stir gently, press slowly.",
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.validationErrors.coffeeGrams")
                .value("Coffee grams must be greater than zero"));
  }

  @Test
  void create_shouldReturnBadRequest_whenWaterGramsIsInvalid() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "Mezcla Veracruz AeroPress",
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(-230),
            "1:15",
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "Bloom 30s, stir gently, press slowly.",
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.validationErrors.waterGrams")
                .value("Water grams must be greater than zero"));
  }

  @Test
  void create_shouldReturnBadRequest_whenWaterTempIsTooLow() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "Mezcla Veracruz AeroPress",
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            69,
            "2:30",
            "Bloom 30s, stir gently, press slowly.",
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.waterTemp")
                .value("Water temperature must be at least 70 degrees Celsius"));
  }

  @Test
  void create_shouldReturnBadRequest_whenWaterTempIsTooHigh() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "Mezcla Veracruz AeroPress",
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            101,
            "2:30",
            "Bloom 30s, stir gently, press slowly.",
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.waterTemp")
                .value("Water temperature must not exceed 100 degrees Celsius"));
  }

  @Test
  void create_shouldReturnBadRequest_whenNameExceedsMaxLength() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "A".repeat(121),
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "Bloom 30s, stir gently, press slowly.",
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.name")
                .value("Recipe name must not exceed 120 characters"));
  }

  @Test
  void create_shouldReturnBadRequest_whenStepsExceedsMaxLength() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "Mezcla Veracruz AeroPress",
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "A".repeat(1001),
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.steps").value("Steps must not exceed 1000 characters"));
  }

  @Test
  void update_shouldReturnBadRequest_whenRecipeNameIsBlank() throws Exception {
    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "",
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "Bloom 30s, stir gently, press slowly.",
            "Clean, aromatic, spicy, balanced.",
            true);

    mockMvc
        .perform(
            put("/api/recipes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.name").value("Recipe name is required"));
  }
}
