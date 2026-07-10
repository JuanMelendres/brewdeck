package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
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
  private final CurrentUserProvider currentUserProvider;

  public RecipeStatsResponse getStats(Long recipeId) {
    Long ownerId = currentOwnerId();

    if (!recipeRepository.existsByIdAndOwnerId(recipeId, ownerId)) {
      throw new EntityNotFoundException(RECIPE_NOT_FOUND);
    }

    RecipeSessionStats stats = brewSessionRepository.findStatsByRecipeId(recipeId, ownerId);

    return new RecipeStatsResponse(
        recipeId, stats.getTotalSessions(), stats.getAverageRating(), stats.getLastBrewedAt());
  }

  public List<TopRatedRecipeResponse> getTopRated(int limit) {
    int safeLimit = clampLimit(limit);

    return brewSessionRepository
        .findTopRated(currentOwnerId(), PageRequest.of(0, safeLimit))
        .stream()
        .map(
            row ->
                new TopRatedRecipeResponse(
                    row.getRecipeId(),
                    row.getRecipeName(),
                    row.getAverageRating(),
                    row.getTotalSessions()))
        .toList();
  }

  public List<MostBrewedRecipeResponse> getMostBrewed(int limit) {
    int safeLimit = clampLimit(limit);

    return brewSessionRepository
        .findMostBrewed(currentOwnerId(), PageRequest.of(0, safeLimit))
        .stream()
        .map(
            row ->
                new MostBrewedRecipeResponse(
                    row.getRecipeId(), row.getRecipeName(), row.getTotalSessions()))
        .toList();
  }

  private int clampLimit(int limit) {
    return Math.min(Math.max(limit, MIN_LIMIT), MAX_LIMIT);
  }

  private Long currentOwnerId() {
    return currentUserProvider.require().getId();
  }
}
