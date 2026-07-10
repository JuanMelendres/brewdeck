package com.brewdeck.brewdeck_api.ai;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.coffee.Coffee;
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

  public SuggestedRecipeResponse improve(Long recipeId) {
    if (!aiProperties.enabled()) {
      throw new AiUnavailableException("AI suggestions are disabled");
    }

    Recipe recipe =
        recipeRepository
            .findByIdAndOwnerId(recipeId, currentUserProvider.require().getId())
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

    SuggestedRecipe suggested = suggestionPort.improve(context);
    log.info(
        "Generated recipe improvement recipeId={} ratedSessions={} model={}",
        recipeId,
        history.size(),
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
