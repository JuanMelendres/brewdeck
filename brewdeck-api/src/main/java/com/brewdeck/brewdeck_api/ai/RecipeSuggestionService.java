package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeSuggestionService {

  private static final String COFFEE_NOT_FOUND = "Coffee not found";
  private static final String BREW_METHOD_NOT_FOUND = "Brew method not found";

  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;
  private final RecipeSuggestionPort suggestionPort;
  private final AiProperties aiProperties;

  public SuggestedRecipeResponse suggest(SuggestRecipeRequest request) {
    if (!aiProperties.enabled()) {
      throw new AiUnavailableException("AI suggestions are disabled");
    }

    Coffee coffee =
        coffeeRepository
            .findById(request.coffeeId())
            .orElseThrow(() -> new EntityNotFoundException(COFFEE_NOT_FOUND));
    BrewMethod method =
        brewMethodRepository
            .findById(request.methodId())
            .orElseThrow(() -> new EntityNotFoundException(BREW_METHOD_NOT_FOUND));

    SuggestionContext context =
        new SuggestionContext(
            coffee.getName(),
            coffee.getOrigin(),
            coffee.getRoastLevel(),
            coffee.getProcess(),
            coffee.getAcidityScore(),
            coffee.getBodyScore(),
            coffee.getSweetnessScore(),
            coffee.getBitternessScore(),
            method.getName(),
            method.getDescription(),
            request.notes());

    SuggestedRecipe suggested = suggestionPort.suggest(context);
    log.info(
        "Generated recipe suggestion coffeeId={} methodId={} model={}",
        request.coffeeId(),
        request.methodId(),
        aiProperties.model());

    return new SuggestedRecipeResponse(
        suggested.coffeeGrams(),
        suggested.waterGrams(),
        suggested.ratio(),
        suggested.grindSetting(),
        suggested.waterTemp(),
        suggested.brewTime(),
        suggested.steps(),
        suggested.rationale());
  }
}
