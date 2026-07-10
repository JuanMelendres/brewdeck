package com.brewdeck.brewdeck_api.dashboard;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;
  private final RecipeRepository recipeRepository;
  private final BrewSessionRepository brewSessionRepository;
  private final CurrentUserProvider currentUserProvider;

  public DashboardSummaryResponse getSummary() {
    Long ownerId = currentUserProvider.require().getId();

    return new DashboardSummaryResponse(
        coffeeRepository.countByOwnerId(ownerId),
        brewMethodRepository.count(),
        recipeRepository.countByOwnerId(ownerId),
        recipeRepository.countByFavoriteTrueAndOwnerId(ownerId),
        brewSessionRepository.countByOwnerId(ownerId),
        brewSessionRepository.findAverageRating(ownerId));
  }
}
