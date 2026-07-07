package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeSuggestionServiceTest {

  @Mock private CoffeeRepository coffeeRepository;
  @Mock private BrewMethodRepository brewMethodRepository;
  @Mock private RecipeSuggestionPort port;

  private RecipeSuggestionService serviceEnabled() {
    return new RecipeSuggestionService(
        coffeeRepository,
        brewMethodRepository,
        port,
        new AiProperties(true, "claude-haiku-4-5", 20, 1024));
  }

  @Test
  void suggest_shouldThrowAiUnavailable_whenDisabled() {
    RecipeSuggestionService service =
        new RecipeSuggestionService(
            coffeeRepository,
            brewMethodRepository,
            port,
            new AiProperties(false, "claude-haiku-4-5", 20, 1024));

    assertThatThrownBy(() -> service.suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(AiUnavailableException.class);
    verify(port, never()).suggest(any());
  }

  @Test
  void suggest_shouldThrowNotFound_whenCoffeeMissing() {
    when(coffeeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> serviceEnabled().suggest(new SuggestRecipeRequest(1L, 2L, null)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void suggest_shouldThrowNotFound_whenBrewMethodMissing() {
    Coffee coffee = Coffee.builder().id(1L).name("Mezcla Veracruz").roastLevel("Medio").build();
    when(coffeeRepository.findById(1L)).thenReturn(Optional.of(coffee));
    when(brewMethodRepository.findById(2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> serviceEnabled().suggest(new SuggestRecipeRequest(1L, 2L, null)))
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
        serviceEnabled().suggest(new SuggestRecipeRequest(1L, 2L, "fruity please"));

    assertThat(result.coffeeGrams()).isEqualByComparingTo("15");
    assertThat(result.waterTemp()).isEqualTo(92);
    assertThat(result.rationale()).isEqualTo("Balanced extraction for a medium roast.");
  }
}
