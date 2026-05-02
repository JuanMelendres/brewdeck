package com.brewdeck.brewdeck_api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrewSessionServiceTest {

  @Mock private BrewSessionRepository brewSessionRepository;
  @Mock private RecipeRepository recipeRepository;

  @InjectMocks private BrewSessionService brewSessionService;

  @Test
  void findAll_shouldReturnAllBrewSessions() {
    Recipe recipe = buildRecipe();

    BrewSession session =
        BrewSession.builder()
            .id(1L)
            .recipe(recipe)
            .brewedAt(LocalDateTime.now())
            .actualGrind("Timemore S3 - 5.5")
            .actualTemp(90)
            .actualTime("2:30")
            .tasteResult("Balanced")
            .rating(9)
            .adjustmentNotes("Repeat same recipe")
            .build();

    when(brewSessionRepository.findAll()).thenReturn(List.of(session));

    List<BrewSessionResponse> result = brewSessionService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo(1L);
    assertThat(result.getFirst().recipeName()).isEqualTo("Veracruz AeroPress");
    assertThat(result.getFirst().rating()).isEqualTo(9);

    verify(brewSessionRepository).findAll();
  }

  @Test
  void findById_shouldReturnBrewSession_whenExists() {
    Recipe recipe = buildRecipe();

    BrewSession session =
        BrewSession.builder().id(1L).recipe(recipe).brewedAt(LocalDateTime.now()).rating(9).build();

    when(brewSessionRepository.findById(1L)).thenReturn(Optional.of(session));

    BrewSessionResponse result = brewSessionService.findById(1L);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.recipeId()).isEqualTo(1L);

    verify(brewSessionRepository).findById(1L);
  }

  @Test
  void findById_shouldThrowException_whenSessionDoesNotExist() {
    when(brewSessionRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewSessionService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Brew session not found");

    verify(brewSessionRepository).findById(99L);
  }

  @Test
  void create_shouldSaveBrewSession_whenRecipeExists() {
    Recipe recipe = buildRecipe();

    BrewSessionRequest request =
        new BrewSessionRequest(
            1L,
            "Timemore S3 - 5.5",
            90,
            "2:30",
            "Balanced, clean and aromatic.",
            9,
            "Repeat same recipe.");

    BrewSession savedSession =
        BrewSession.builder()
            .id(1L)
            .recipe(recipe)
            .brewedAt(LocalDateTime.now())
            .actualGrind(request.actualGrind())
            .actualTemp(request.actualTemp())
            .actualTime(request.actualTime())
            .tasteResult(request.tasteResult())
            .rating(request.rating())
            .adjustmentNotes(request.adjustmentNotes())
            .build();

    when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
    when(brewSessionRepository.save(any(BrewSession.class))).thenReturn(savedSession);

    BrewSessionResponse result = brewSessionService.create(request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.recipeName()).isEqualTo("Veracruz AeroPress");
    assertThat(result.rating()).isEqualTo(9);

    verify(recipeRepository).findById(1L);
    verify(brewSessionRepository).save(any(BrewSession.class));
  }

  @Test
  void create_shouldThrowException_whenRecipeDoesNotExist() {
    BrewSessionRequest request = new BrewSessionRequest(99L, null, null, null, null, null, null);

    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewSessionService.create(request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(recipeRepository).findById(99L);
    verify(brewSessionRepository, never()).save(any());
  }

  @Test
  void findByRecipeId_shouldReturnSessionsOrderedByBrewedAtDesc() {
    Recipe recipe = buildRecipe();

    BrewSession session =
        BrewSession.builder().id(1L).recipe(recipe).brewedAt(LocalDateTime.now()).rating(9).build();

    when(brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(1L)).thenReturn(List.of(session));

    List<BrewSessionResponse> result = brewSessionService.findByRecipeId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().recipeId()).isEqualTo(1L);

    verify(brewSessionRepository).findByRecipeIdOrderByBrewedAtDesc(1L);
  }

  private Recipe buildRecipe() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").build();
    BrewMethod method = BrewMethod.builder().id(1L).name("AeroPress").build();

    return Recipe.builder()
        .id(1L)
        .coffee(coffee)
        .method(method)
        .name("Veracruz AeroPress")
        .favorite(true)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void update_shouldUpdateBrewSession_whenSessionAndRecipeExist() {
    Recipe oldRecipe = buildRecipe();
    Recipe newRecipe = buildRecipe();

    BrewSession existingSession =
        BrewSession.builder()
            .id(1L)
            .recipe(oldRecipe)
            .brewedAt(LocalDateTime.now())
            .actualGrind("Old grind")
            .actualTemp(88)
            .actualTime("2:00")
            .tasteResult("Old result")
            .rating(7)
            .adjustmentNotes("Old notes")
            .build();

    BrewSessionRequest request =
        new BrewSessionRequest(
            1L,
            "Timemore S3 - 5.8",
            91,
            "2:40",
            "More aromatic and balanced.",
            10,
            "Repeat this version.");

    when(brewSessionRepository.findById(1L)).thenReturn(Optional.of(existingSession));
    when(recipeRepository.findById(1L)).thenReturn(Optional.of(newRecipe));
    when(brewSessionRepository.save(existingSession)).thenReturn(existingSession);

    BrewSessionResponse result = brewSessionService.update(1L, request);

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.recipeId()).isEqualTo(1L);
    assertThat(result.actualGrind()).isEqualTo("Timemore S3 - 5.8");
    assertThat(result.actualTemp()).isEqualTo(91);
    assertThat(result.actualTime()).isEqualTo("2:40");
    assertThat(result.tasteResult()).isEqualTo("More aromatic and balanced.");
    assertThat(result.rating()).isEqualTo(10);
    assertThat(result.adjustmentNotes()).isEqualTo("Repeat this version.");

    verify(brewSessionRepository).findById(1L);
    verify(recipeRepository).findById(1L);
    verify(brewSessionRepository).save(existingSession);
  }

  @Test
  void update_shouldThrowException_whenBrewSessionDoesNotExist() {
    BrewSessionRequest request =
        new BrewSessionRequest(
            1L,
            "Timemore S3 - 5.8",
            91,
            "2:40",
            "More aromatic and balanced.",
            10,
            "Repeat this version.");

    when(brewSessionRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewSessionService.update(99L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Brew session not found");

    verify(brewSessionRepository).findById(99L);
    verify(recipeRepository, never()).findById(anyLong());
    verify(brewSessionRepository, never()).save(any());
  }

  @Test
  void update_shouldThrowException_whenRecipeDoesNotExist() {
    Recipe oldRecipe = buildRecipe();

    BrewSession existingSession =
        BrewSession.builder()
            .id(1L)
            .recipe(oldRecipe)
            .brewedAt(LocalDateTime.now())
            .rating(8)
            .build();

    BrewSessionRequest request =
        new BrewSessionRequest(
            99L,
            "Timemore S3 - 5.8",
            91,
            "2:40",
            "More aromatic and balanced.",
            10,
            "Repeat this version.");

    when(brewSessionRepository.findById(1L)).thenReturn(Optional.of(existingSession));
    when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brewSessionService.update(1L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(brewSessionRepository).findById(1L);
    verify(recipeRepository).findById(99L);
    verify(brewSessionRepository, never()).save(any());
  }

  @Test
  void delete_shouldDeleteBrewSession_whenSessionExists() {
    when(brewSessionRepository.existsById(1L)).thenReturn(true);

    brewSessionService.delete(1L);

    verify(brewSessionRepository).existsById(1L);
    verify(brewSessionRepository).deleteById(1L);
  }

  @Test
  void delete_shouldThrowException_whenBrewSessionDoesNotExist() {
    when(brewSessionRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> brewSessionService.delete(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Brew session not found");

    verify(brewSessionRepository).existsById(99L);
    verify(brewSessionRepository, never()).deleteById(anyLong());
  }
}
