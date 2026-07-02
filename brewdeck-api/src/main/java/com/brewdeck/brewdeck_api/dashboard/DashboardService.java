package com.brewdeck.brewdeck_api.dashboard;

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

  public DashboardSummaryResponse getSummary() {
    return new DashboardSummaryResponse(
        coffeeRepository.count(),
        brewMethodRepository.count(),
        recipeRepository.count(),
        recipeRepository.countByFavoriteTrue(),
        brewSessionRepository.count(),
        brewSessionRepository.findAverageRating());
  }
}
