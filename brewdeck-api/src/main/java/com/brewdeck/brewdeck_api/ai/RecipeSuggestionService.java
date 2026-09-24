package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
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
  private final FeatureFlagService featureFlagService;
  private final CurrentUserProvider currentUserProvider;

  public SuggestedRecipeResponse suggest(SuggestRecipeRequest request) {
    // Release gate: the AI assistant is the source of truth here, not the frontend hiding buttons.
    // Checked before loading any data or calling the external provider. This RELEASE flag runs at
    // 0/100 rollout, so an empty (user-less) context is sufficient. A disabled flag yields 404.
    // (The provider-availability concern — is a real model wired — remains the port's job: the
    // disabled adapter still throws AiUnavailable -> 503 when the flag is on but AI is not.)
    featureFlagService.requireEnabled(FeatureKeys.AI_RECIPE_ASSISTANT);

    // Both lookups are scoped to the caller: another user's coffee or private method is a 404,
    // so their data can never reach the AI prompt.
    Long ownerId = currentUserProvider.require().getId();
    Coffee coffee =
        coffeeRepository
            .findByIdAndOwnerId(request.coffeeId(), ownerId)
            .orElseThrow(() -> new EntityNotFoundException(COFFEE_NOT_FOUND));
    BrewMethod method =
        brewMethodRepository
            .findVisibleById(request.methodId(), ownerId)
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
