package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.brewdeck.brewdeck_api.session.RecipeSessionStats;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeStatsService {

  private static final String RECIPE_NOT_FOUND = "Recipe not found";
  private static final int MIN_LIMIT = 1;
  private static final int MAX_LIMIT = 20;

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

  public List<TopRatedRecipeResponse> getTopRated(int limit) {
    int safeLimit = Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);

    return brewSessionRepository.findTopRated(PageRequest.of(0, safeLimit)).stream()
        .map(
            row ->
                new TopRatedRecipeResponse(
                    row.getRecipeId(),
                    row.getRecipeName(),
                    row.getAverageRating(),
                    row.getTotalSessions()))
        .toList();
  }
}
