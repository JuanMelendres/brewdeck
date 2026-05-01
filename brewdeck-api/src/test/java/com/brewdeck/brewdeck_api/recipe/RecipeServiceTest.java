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
}
