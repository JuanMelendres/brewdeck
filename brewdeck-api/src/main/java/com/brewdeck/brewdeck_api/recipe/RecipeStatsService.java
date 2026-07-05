package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.brewdeck.brewdeck_api.session.RecipeSessionStats;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeStatsService {

  private static final String RECIPE_NOT_FOUND = "Recipe not found";

  private final RecipeRepository recipeRepository;
  private final BrewSessionRepository brewSessionRepository;

  public RecipeStatsResponse getStats(Long recipeId) {
    if (!recipeRepository.existsById(recipeId)) {
      throw new EntityNotFoundException(RECIPE_NOT_FOUND);
    }

    RecipeSessionStats stats = brewSessionRepository.findStatsByRecipeId(recipeId);

    return new RecipeStatsResponse(
        recipeId, stats.getTotalSessions(), stats.getAverageRating(), stats.getLastBrewedAt());
  }
}
