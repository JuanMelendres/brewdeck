package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PageResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

  @Mock private RecipeRepository recipeRepository;
  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;

  @InjectMocks private RecipeService recipeService;

  @Test
  void search_shouldReturnPagedRecipes_whenFilterIsEmpty() {
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

    Pageable pageable = PageRequest.of(0, 10);

    when(recipeRepository.findAll(anyRecipeSpecification(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));

    PageResponse<RecipeResponse> result =
        recipeService.search(new RecipeFilter(null, null, null, null), pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().id()).isEqualTo(1L);
    assertThat(result.content().getFirst().coffeeName()).isEqualTo("Mezcla Veracruz");
    assertThat(result.content().getFirst().methodName()).isEqualTo("AeroPress");
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalElements()).isEqualTo(1);

    verify(recipeRepository).findAll(anyRecipeSpecification(), eq(pageable));
  }

  @Test
  void search_shouldReturnPagedFilteredRecipes() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    Recipe recipe =
        Recipe.builder()
            .id(1L)
            .coffee(coffee)
            .method(method)
            .name("Mezcla Veracruz AeroPress")
            .favorite(true)
            .createdAt(LocalDateTime.now())
            .build();

    RecipeFilter filter = new RecipeFilter(1L, 1L, true, "AeroPress");
    Pageable pageable = PageRequest.of(0, 5);

    when(recipeRepository.findAll(anyRecipeSpecification(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));

    PageResponse<RecipeResponse> result = recipeService.search(filter, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().coffeeId()).isEqualTo(1L);
    assertThat(result.content().getFirst().methodId()).isEqualTo(1L);
    assertThat(result.content().getFirst().favorite()).isTrue();
    assertThat(result.content().getFirst().name()).isEqualTo("Mezcla Veracruz AeroPress");
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(5);
    assertThat(result.totalElements()).isEqualTo(1);

    verify(recipeRepository).findAll(anyRecipeSpecification(), eq(pageable));
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
  void findFavorites_shouldReturnPagedFavoriteRecipes() {
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

    Pageable pageable = PageRequest.of(0, 10);

    when(recipeRepository.findByFavoriteTrue(pageable))
        .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));

    PageResponse<RecipeResponse> result = recipeService.findFavorites(pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().favorite()).isTrue();
    assertThat(result.totalElements()).isEqualTo(1);

    verify(recipeRepository).findByFavoriteTrue(pageable);
  }

  @Test
  void findByCoffeeId_shouldReturnPagedRecipesForCoffee() {
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

    Pageable pageable = PageRequest.of(0, 10);

    when(recipeRepository.findByCoffeeId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));

    PageResponse<RecipeResponse> result = recipeService.findByCoffeeId(1L, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().coffeeId()).isEqualTo(1L);
    assertThat(result.content().getFirst().coffeeName()).isEqualTo("Mezcla Veracruz");

    verify(recipeRepository).findByCoffeeId(1L, pageable);
  }

  @Test
  void findByMethodId_shouldReturnPagedRecipesForMethod() {
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

    Pageable pageable = PageRequest.of(0, 10);

    when(recipeRepository.findByMethodId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));

    PageResponse<RecipeResponse> result = recipeService.findByMethodId(1L, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().methodId()).isEqualTo(1L);
    assertThat(result.content().getFirst().methodName()).isEqualTo("AeroPress");

    verify(recipeRepository).findByMethodId(1L, pageable);
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

  @Test
  void markAsFavorite_shouldSetFavoriteTrue_whenRecipeExists() {
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
    when(recipeRepository.save(recipe)).thenReturn(recipe);

    RecipeResponse result = recipeService.markAsFavorite(1L);

    assertThat(result.favorite()).isTrue();

    verify(recipeRepository).findById(1L);
    verify(recipeRepository).save(recipe);
  }

  @Test
  void markAsFavorite_shouldThrowException_whenRecipeDoesNotExist() {
    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.markAsFavorite(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(recipeRepository).findById(99L);
    verify(recipeRepository, never()).save(any());
  }

  @Test
  void removeFromFavorites_shouldSetFavoriteFalse_whenRecipeExists() {
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

    when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
    when(recipeRepository.save(recipe)).thenReturn(recipe);

    RecipeResponse result = recipeService.removeFromFavorites(1L);

    assertThat(result.favorite()).isFalse();

    verify(recipeRepository).findById(1L);
    verify(recipeRepository).save(recipe);
  }

  @Test
  void removeFromFavorites_shouldThrowException_whenRecipeDoesNotExist() {
    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recipeService.removeFromFavorites(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(recipeRepository).findById(99L);
    verify(recipeRepository, never()).save(any());
  }

  @SuppressWarnings("unchecked")
  private Specification<Recipe> anyRecipeSpecification() {
    return any(Specification.class);
  }
}
