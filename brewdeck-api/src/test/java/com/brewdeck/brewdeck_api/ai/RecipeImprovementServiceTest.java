package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeImprovementServiceTest {

  @Mock private RecipeRepository recipeRepository;
  @Mock private BrewSessionRepository brewSessionRepository;
  @Mock private RecipeSuggestionPort port;

  private RecipeImprovementService serviceEnabled() {
    return new RecipeImprovementService(
        recipeRepository,
        brewSessionRepository,
        port,
        new AiProperties(true, "claude-haiku-4-5", 20, 1024));
  }

  private Recipe sampleRecipe() {
    Coffee coffee =
        Coffee.builder()
            .id(1L)
            .name("Mezcla Veracruz")
            .origin("Veracruz")
            .process("Lavado")
            .roastLevel("Medio")
            .acidityScore(3)
            .bodyScore(3)
            .sweetnessScore(3)
            .bitternessScore(2)
            .build();
    BrewMethod method =
        BrewMethod.builder().id(2L).name("AeroPress").description("Immersion").build();
    return Recipe.builder()
        .id(5L)
        .coffee(coffee)
        .method(method)
        .name("Mezcla AeroPress")
        .coffeeGrams(new BigDecimal("15"))
        .waterGrams(new BigDecimal("230"))
        .ratio("1:15")
        .grindSetting("Timemore S3 - 5.5")
        .waterTemp(90)
        .brewTime("2:30")
        .steps("Bloom then press.")
        .favorite(false)
        .build();
  }

  @Test
  void improve_shouldThrowAiUnavailable_whenDisabled() {
    RecipeImprovementService service =
        new RecipeImprovementService(
            recipeRepository,
            brewSessionRepository,
            port,
            new AiProperties(false, "claude-haiku-4-5", 20, 1024));

    assertThatThrownBy(() -> service.improve(5L)).isInstanceOf(AiUnavailableException.class);
    verify(port, never()).improve(any());
  }

  @Test
  void improve_shouldThrowNotFound_whenRecipeMissing() {
    when(recipeRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> serviceEnabled().improve(5L))
        .isInstanceOf(EntityNotFoundException.class);
    verify(port, never()).improve(any());
  }

  @Test
  void improve_shouldThrowInsufficientHistory_whenNoRatedSessions() {
    when(recipeRepository.findById(5L)).thenReturn(Optional.of(sampleRecipe()));
    when(brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(5L))
        .thenReturn(List.of());

    assertThatThrownBy(() -> serviceEnabled().improve(5L))
        .isInstanceOf(InsufficientBrewHistoryException.class);
    verify(port, never()).improve(any());
  }

  @Test
  void improve_shouldReturnMappedResponse_whenHistoryPresent() {
    Recipe recipe = sampleRecipe();
    BrewSession session =
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
            .actualGrind("Timemore S3 - 5.5")
            .actualTemp(91)
            .actualTime("2:20")
            .tasteResult("Bright")
            .rating(9)
            .adjustmentNotes("Grind finer next time.")
            .build();
    when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
    when(brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(5L))
        .thenReturn(List.of(session));
    when(port.improve(any()))
        .thenReturn(
            new SuggestedRecipe(
                new BigDecimal("16"),
                new BigDecimal("240"),
                "1:15",
                "Timemore S3 - 5.0",
                92,
                "2:15",
                "Grind finer and shorten the brew.",
                "Finer grind improves sweetness."));

    ArgumentCaptor<ImprovementContext> captor = ArgumentCaptor.forClass(ImprovementContext.class);

    SuggestedRecipeResponse result = serviceEnabled().improve(5L);

    verify(port).improve(captor.capture());
    ImprovementContext context = captor.getValue();
    assertThat(context.coffeeName()).isEqualTo("Mezcla Veracruz");
    assertThat(context.methodName()).isEqualTo("AeroPress");
    assertThat(context.currentWaterTemp()).isEqualTo(90);
    assertThat(context.history()).hasSize(1);
    assertThat(context.history().get(0).rating()).isEqualTo(9);
    assertThat(context.history().get(0).tasteResult()).isEqualTo("Bright");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Finer grind improves sweetness.");
  }
}
