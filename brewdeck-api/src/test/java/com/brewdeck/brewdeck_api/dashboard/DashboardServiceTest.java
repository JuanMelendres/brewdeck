package com.brewdeck.brewdeck_api.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  @Mock private CoffeeRepository coffeeRepository;

  @Mock private BrewMethodRepository brewMethodRepository;

  @Mock private RecipeRepository recipeRepository;

  @Mock private BrewSessionRepository brewSessionRepository;

  @Mock private CurrentUserProvider currentUserProvider;

  @InjectMocks private DashboardService dashboardService;

  @Test
  void getSummary_shouldAggregateCounts() {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.countByOwnerId(42L)).thenReturn(5L);
    when(brewMethodRepository.count()).thenReturn(4L);
    when(recipeRepository.countByOwnerId(42L)).thenReturn(10L);
    when(recipeRepository.countByFavoriteTrueAndOwnerId(42L)).thenReturn(3L);
    when(brewSessionRepository.countByOwnerId(42L)).thenReturn(20L);
    when(brewSessionRepository.findAverageRating(42L)).thenReturn(4.25);

    DashboardSummaryResponse summary = dashboardService.getSummary();

    assertThat(summary.totalCoffees()).isEqualTo(5L);
    assertThat(summary.totalBrewMethods()).isEqualTo(4L);
    assertThat(summary.totalRecipes()).isEqualTo(10L);
    assertThat(summary.favoriteRecipes()).isEqualTo(3L);
    assertThat(summary.totalBrewSessions()).isEqualTo(20L);
    assertThat(summary.averageSessionRating()).isEqualTo(4.25);
  }

  @Test
  void getSummary_shouldReturnNullAverage_whenNoRatings() {
    when(currentUserProvider.require()).thenReturn(User.builder().id(42L).build());
    when(coffeeRepository.countByOwnerId(42L)).thenReturn(0L);
    when(brewMethodRepository.count()).thenReturn(0L);
    when(recipeRepository.countByOwnerId(42L)).thenReturn(0L);
    when(recipeRepository.countByFavoriteTrueAndOwnerId(42L)).thenReturn(0L);
    when(brewSessionRepository.countByOwnerId(42L)).thenReturn(0L);
    when(brewSessionRepository.findAverageRating(42L)).thenReturn(null);

    DashboardSummaryResponse summary = dashboardService.getSummary();

    assertThat(summary.totalBrewSessions()).isZero();
    assertThat(summary.averageSessionRating()).isNull();
  }
}
