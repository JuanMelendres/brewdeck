package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimitRule;
import com.brewdeck.brewdeck_api.common.ratelimit.RateLimiter;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/*
 * Deliberately NOT @Transactional: this service calls an external LLM (up to
 * brewdeck.ai.timeout-seconds), and a surrounding transaction would hold a pooled DB connection for
 * that whole time. The repository reads run in their own short read-only transactions and fetch
 * everything the prompt needs up front: the recipe arrives with its coffee and method via
 * @EntityGraph, so nothing lazy-loads after them (open-in-view is off).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeImprovementService {

  private static final String RECIPE_NOT_FOUND = "Recipe not found";
  private static final String NO_RATED_HISTORY = "Recipe has no rated brew sessions";

  private final RecipeRepository recipeRepository;
  private final BrewSessionRepository brewSessionRepository;
  private final RecipeSuggestionPort suggestionPort;
  private final AiProperties aiProperties;
  private final CurrentUserProvider currentUserProvider;
  private final FeatureFlagService featureFlagService;
  private final RateLimiter rateLimiter;
  private final SuggestionGuardrails guardrails;

  public SuggestedRecipeResponse improve(Long recipeId) {
    // Release gate before any work: see RecipeSuggestionService for the flag-vs-provider rationale.
    featureFlagService.requireEnabled(FeatureKeys.AI_RECIPE_ASSISTANT);
    Long ownerId = currentUserProvider.require().getId();
    // Each call ties up the model for many seconds (local) or costs money (hosted).
    rateLimiter.requireAllowed(RateLimitRule.AI_ASSISTANT_USER, String.valueOf(ownerId));

    Recipe recipe =
        recipeRepository
            .findByIdAndOwnerId(recipeId, ownerId)
            .orElseThrow(() -> new EntityNotFoundException(RECIPE_NOT_FOUND));

    List<BrewSession> sessions =
        brewSessionRepository.findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(recipeId);
    if (sessions.isEmpty()) {
      throw new InsufficientBrewHistoryException(NO_RATED_HISTORY);
    }

    List<BrewHistoryEntry> history =
        sessions.stream()
            .map(
                session ->
                    new BrewHistoryEntry(
                        session.getRating(),
                        session.getActualGrind(),
                        session.getActualTemp(),
                        session.getActualTime(),
                        session.getTasteResult(),
                        session.getAdjustmentNotes()))
            .toList();

    Coffee coffee = recipe.getCoffee();
    BrewMethod method = recipe.getMethod();

    ImprovementContext context =
        new ImprovementContext(
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
            recipe.getCoffeeGrams(),
            recipe.getWaterGrams(),
            recipe.getRatio(),
            recipe.getGrindSetting(),
            recipe.getWaterTemp(),
            recipe.getBrewTime(),
            recipe.getSteps(),
            history);

    SuggestedRecipe suggested = guardrails.apply(method.getName(), suggestionPort.improve(context));
    log.info(
        "Generated recipe improvement recipeId={} ratedSessions={} model={} promptVersion={}",
        recipeId,
        history.size(),
        aiProperties.activeModel(),
        RecipePrompts.VERSION);

    return new SuggestedRecipeResponse(
        suggested.coffeeGrams(),
        suggested.waterGrams(),
        suggested.ratio(),
        suggested.grindSetting(),
        suggested.waterTemp(),
        suggested.brewTime(),
        suggested.steps(),
        suggested.rationale(),
        aiProperties.activeModel(),
        RecipePrompts.VERSION);
  }
}
