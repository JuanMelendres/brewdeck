package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.featureflag.FeatureDisabledException;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RecipeSuggestionServiceTest {

  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;
  @Mock private RecipeSuggestionPort port;
  @Mock private FeatureFlagService featureFlagService;

  // A plain Mockito mock leaves the void requireEnabled as a no-op, i.e. the flag is enabled.
  private RecipeSuggestionService service() {
    return new RecipeSuggestionService(
        coffeeRepository,
        brewMethodRepository,
        port,
        new AiProperties(true, "claude-haiku-4-5", 20, 1024),
        featureFlagService);
  }

  @Test
  void suggest_shouldThrowFeatureDisabled_whenFlagOff() {
    doThrow(new FeatureDisabledException(FeatureKeys.AI_RECIPE_ASSISTANT, HttpStatus.NOT_FOUND))
        .when(featureFlagService)
        .requireEnabled(FeatureKeys.AI_RECIPE_ASSISTANT);

    RecipeSuggestionService service = service();

    assertThatThrownBy(() -> service.suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(FeatureDisabledException.class);
    verify(port, never()).suggest(any());
  }

  @Test
  void suggest_shouldThrowNotFound_whenCoffeeMissing() {
    when(coffeeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void suggest_shouldThrowNotFound_whenBrewMethodMissing() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").roastLevel("Medio").build();
    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));
    when(brewMethodRepository.findById(2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void suggest_shouldReturnMappedResponse_whenEnabled() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").roastLevel("Medio").build();
    BrewMethod method = BrewMethod.builder().id(2L).name("AeroPress").build();
    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));
    when(brewMethodRepository.findById(2L)).thenReturn(Optional.of(method));
    when(port.suggest(any()))
        .thenReturn(
            new SuggestedRecipe(
                new BigDecimal("15"),
                new BigDecimal("240"),
                "1:16",
                "Medium-fine",
                92,
                "2:30",
                "Bloom then pour.",
                "Balanced extraction for a medium roast."));

    SuggestedRecipeResponse result =
        service().suggest(new SuggestRecipeRequest(1L, 2L, "fruity please"));

    assertThat(result.coffeeGrams()).isEqualByComparingTo("15");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Balanced extraction for a medium roast.");
  }
}
