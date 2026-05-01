package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

  @Mock private RecipeRepository recipeRepository;
  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;

  @InjectMocks private RecipeService recipeService;

  @Test
  void findAll_shouldReturnAllRecipes() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe recipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name("Veracruz AeroPress")
            .favorite(true)
            .createdAt(LocalDateTime.now())
            .build();

    when(recipeRepository.findAll()).thenReturn(List.of(recipe));

    List<RecipeResponse> result = recipeService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(1L);
    assertThat(result.getFirst().coffeeName()).isEqualTo("Mezcla Veracruz");
    assertThat(result.getFirst().methodName()).isEqualTo("AeroPress");

    verify(recipeRepository).findAll();
  }

  @Test
  void findById_shouldReturnRecipe_whenExists() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe recipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name("Veracruz AeroPress")
            .favorite(false)
            .createdAt(LocalDateTime.now())
            .build();

    when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

    RecipeResponse result = recipeService.findById(1L);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Veracruz AeroPress");

    verify(recipeRepository).findById(1L);
  }

  @Test
  void findById_shouldThrowException_whenRecipeDoesNotExist() {
    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(recipeRepository).findById(99L);
  }

  @Test
  void create_shouldSaveRecipe_whenCoffeeAndMethodExist() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    RecipeRequest request =
        new RecipeRequest(
            1L,
            1L,
            "Veracruz AeroPress",
            BigDecimal.valueOf(15),
            BigDecimal.valueOf(230),
            "1:15",
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "Bloom 30s, stir, press slowly.",
            "Clean, aromatic, balanced.",
            true);

    Recipe savedRecipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name(request.name())
            .coffeeGrams(request.coffeeGrams())
            .waterGrams(request.waterGrams())
            .ratio(request.ratio())
            .grindSetting(request.grindSetting())
            .waterTemp(request.waterTemp())
            .brewTime(request.brewTime())
            .steps(request.steps())
            .expectedTaste(request.expectedTaste())
            .favorite(true)
            .createdAt(LocalDateTime.now())
            .build();

    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));
    when(brewMethodRepository.findById(1L)).thenReturn(Optional.of(method));
    when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);

    RecipeResponse result = recipeService.create(request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Veracruz AeroPress");
    assertThat(result.favorite()).isTrue();

    verify(coffeeRepository).findById(1L);
    verify(brewMethodRepository).findById(1L);
    verify(recipeRepository).save(any(Recipe.class));
  }

  @Test
  void create_shouldThrowException_whenCoffeeDoesNotExist() {
    RecipeRequest request =
        new RecipeRequest(99L, 1L, "Recipe", null, null, null, null, null, null, null, null, false);

    when(coffeeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.create(request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(coffeeRepository).findById(99L);
    verify(brewMethodRepository, never()).findById(anyLong());
    verify(recipeRepository, never()).save(any());
  }

  @Test
  void findFavorites_shouldReturnFavoriteRecipes() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe recipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name("Favorite Recipe")
            .favorite(true)
            .createdAt(LocalDateTime.now())
            .build();

    when(recipeRepository.findByFavoriteTrue()).thenReturn(List.of(recipe));

    List<RecipeResponse> result = recipeService.findFavorites();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().favorite()).isTrue();

    verify(recipeRepository).findByFavoriteTrue();
  }

  @Test
  void findByCoffeeId_shouldReturnRecipesForCoffee() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe recipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name("Veracruz AeroPress")
            .favorite(false)
            .createdAt(LocalDateTime.now())
            .build();

    when(recipeRepository.findByCoffeeId(1L)).thenReturn(List.of(recipe));

    List<RecipeResponse> result = recipeService.findByCoffeeId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().coffeeId()).isEqualTo(1L);
    assertThat(result.getFirst().coffeeName()).isEqualTo("Mezcla Veracruz");

    verify(recipeRepository).findByCoffeeId(1L);
  }

  @Test
  void findByMethodId_shouldReturnRecipesForMethod() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe recipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name("Veracruz AeroPress")
            .favorite(false)
            .createdAt(LocalDateTime.now())
            .build();

    when(recipeRepository.findByMethodId(1L)).thenReturn(List.of(recipe));

    List<RecipeResponse> result = recipeService.findByMethodId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().methodId()).isEqualTo(1L);
    assertThat(result.getFirst().methodName()).isEqualTo("AeroPress");

    verify(recipeRepository).findByMethodId(1L);
  }

  @Test
  void update_shouldUpdateRecipe_whenRecipeCoffeeAndMethodExist() {
    Coffee oldCoffee = Coffee.builder().id(1L).name("Old Coffee").build();
    BrewMethod oldMethod = BrewMethod.builder().id(1L).name("Old Method").build();

    Coffee newCoffee = Coffee.builder().id(2L).name("Mezcla Veracruz").build();
    BrewMethod newMethod = BrewMethod.builder().id(2L).name("AeroPress").build();

    Recipe existingRecipe =
        Recipe.builder()
            .id(1L)
            .coffee(oldCoffee)
            .method(oldMethod)
            .name("Old Recipe")
            .favorite(false)
            .createdAt(LocalDateTime.now())
            .build();

    RecipeRequest request =
        new RecipeRequest(
            2L,
            2L,
            "Mezcla Veracruz AeroPress Updated",
            BigDecimal.valueOf(16),
            BigDecimal.valueOf(240),
            "1:15",
            "Timemore S3 - 5.8",
            91,
            "2:40",
            "Updated steps.",
            "Updated expected taste.",
            true);

    when(recipeRepository.findById(1L)).thenReturn(Optional.of(existingRecipe));
    when(coffeeRepository.findById(2L)).thenReturn(Optional.of(newCoffee));
    when(brewMethodRepository.findById(2L)).thenReturn(Optional.of(newMethod));
    when(recipeRepository.save(existingRecipe)).thenReturn(existingRecipe);

    RecipeResponse result = recipeService.update(1L, request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.coffeeId()).isEqualTo(2L);
    assertThat(result.methodId()).isEqualTo(2L);
    assertThat(result.name()).isEqualTo("Mezcla Veracruz AeroPress Updated");
    assertThat(result.favorite()).isTrue();
    assertThat(result.grindSetting()).isEqualTo("Timemore S3 - 5.8");

    verify(recipeRepository).findById(1L);
    verify(coffeeRepository).findById(2L);
    verify(brewMethodRepository).findById(2L);
    verify(recipeRepository).save(existingRecipe);
  }

  @Test
  void update_shouldThrowException_whenRecipeDoesNotExist() {
    RecipeRequest request =
        new RecipeRequest(1L, 1L, "Recipe", null, null, null, null, null, null, null, null, false);

    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.update(99L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(recipeRepository).findById(99L);
    verify(coffeeRepository, never()).findById(anyLong());
    verify(brewMethodRepository, never()).findById(anyLong());
    verify(recipeRepository, never()).save(any());
  }

  @Test
  void update_shouldThrowException_whenCoffeeDoesNotExist() {
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe existingRecipe =
        Recipe.builder()
            .id(1L)
            .method(method)
            .name("Existing Recipe")
            .favorite(false)
            .createdAt(LocalDateTime.now())
            .build();

    RecipeRequest request =
        new RecipeRequest(99L, 1L, "Recipe", null, null, null, null, null, null, null, null, false);

    when(recipeRepository.findById(1L)).thenReturn(Optional.of(existingRecipe));
    when(coffeeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.update(1L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Coffee not found");

    verify(recipeRepository).findById(1L);
    verify(coffeeRepository).findById(99L);
    verify(brewMethodRepository, never()).findById(anyLong());
    verify(recipeRepository, never()).save(any());
  }

  @Test
  void update_shouldThrowException_whenMethodDoesNotExist() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod oldMethod = BrewMethod.builder().id(1L).name("Old Method").build();

    Recipe existingRecipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(oldMethod)
            .name("Existing Recipe")
            .favorite(false)
            .createdAt(LocalDateTime.now())
            .build();

    RecipeRequest request =
        new RecipeRequest(1L, 99L, "Recipe", null, null, null, null, null, null, null, null, false);

    when(recipeRepository.findById(1L)).thenReturn(Optional.of(existingRecipe));
    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));
    when(brewMethodRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.update(1L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Brew method not found");

    verify(recipeRepository).findById(1L);
    verify(coffeeRepository).findById(1L);
    verify(brewMethodRepository).findById(99L);
    verify(recipeRepository, never()).save(any());
  }

  @Test
  void delete_shouldDeleteRecipe_whenRecipeExists() {
    when(recipeRepository.existsById(1L)).thenReturn(true);

    recipeService.delete(1L);

    verify(recipeRepository).existsById(1L);
    verify(recipeRepository).deleteById(1L);
  }

  @Test
  void delete_shouldThrowException_whenRecipeDoesNotExist() {
    when(recipeRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> recipeService.delete(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(recipeRepository).existsById(99L);
    verify(recipeRepository, never()).deleteById(anyLong());
  }
}
