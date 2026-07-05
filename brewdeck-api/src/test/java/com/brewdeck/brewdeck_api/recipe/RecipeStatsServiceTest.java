package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.brewdeck.brewdeck_api.session.RecipeSessionStats;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeStatsServiceTest {

  @Mock private RecipeRepository recipeRepository;

  @Mock private BrewSessionRepository brewSessionRepository;

  @InjectMocks private RecipeStatsService recipeStatsService;

  @Test
  void getStats_shouldReturnAggregatedStats() {
    LocalDateTime lastBrewedAt = LocalDateTime.of(2026, 7, 5, 10, 0);

    when(recipeRepository.existsById(1L)).thenReturn(true);
    when(brewSessionRepository.findStatsByRecipeId(1L)).thenReturn(stats(3L, 8.5, lastBrewedAt));

    RecipeStatsResponse response = recipeStatsService.getStats(1L);

    assertThat(response.recipeId()).isEqualTo(1L);
    assertThat(response.totalSessions()).isEqualTo(3L);
    assertThat(response.averageRating()).isEqualTo(8.5);
    assertThat(response.lastBrewedAt()).isEqualTo(lastBrewedAt);
  }

  @Test
  void getStats_shouldReturnZerosAndNulls_whenNoSessions() {
    when(recipeRepository.existsById(1L)).thenReturn(true);
    when(brewSessionRepository.findStatsByRecipeId(1L)).thenReturn(stats(0L, null, null));

    RecipeStatsResponse response = recipeStatsService.getStats(1L);

    assertThat(response.recipeId()).isEqualTo(1L);
    assertThat(response.totalSessions()).isZero();
    assertThat(response.averageRating()).isNull();
    assertThat(response.lastBrewedAt()).isNull();
  }

  @Test
  void getStats_shouldThrow_whenRecipeMissing() {
    when(recipeRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> recipeStatsService.getStats(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Recipe not found");

    verify(brewSessionRepository, never()).findStatsByRecipeId(99L);
  }

  private RecipeSessionStats stats(long totalSessions, Double averageRating, LocalDateTime last) {
    return new RecipeSessionStats() {
      @Override
      public long getTotalSessions() {
        return totalSessions;
      }

      @Override
      public Double getAverageRating() {
        return averageRating;
      }

      @Override
      public LocalDateTime getLastBrewedAt() {
        return last;
      }
    };
  }
}
