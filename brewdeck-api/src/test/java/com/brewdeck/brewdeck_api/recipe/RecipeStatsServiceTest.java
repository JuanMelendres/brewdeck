package com.brewdeck.brewdeck_api.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.brewdeck.brewdeck_api.session.MostBrewedRecipe;
import com.brewdeck.brewdeck_api.session.RecipeSessionStats;
import com.brewdeck.brewdeck_api.session.TopRatedRecipe;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

  @Test
  void getTopRated_shouldMapRowsPreservingOrder() {
    when(brewSessionRepository.findTopRated(any(Pageable.class)))
        .thenReturn(List.of(topRated(2L, "Best", 9.0, 4L), topRated(1L, "Good", 7.5, 2L)));

    List<TopRatedRecipeResponse> response = recipeStatsService.getTopRated(5);

    assertThat(response).hasSize(2);
    assertThat(response.get(0).recipeId()).isEqualTo(2L);
    assertThat(response.get(0).recipeName()).isEqualTo("Best");
    assertThat(response.get(0).averageRating()).isEqualTo(9.0);
    assertThat(response.get(0).totalSessions()).isEqualTo(4L);
    assertThat(response.get(1).recipeId()).isEqualTo(1L);
  }

  @Test
  void getTopRated_shouldClampLimitToRange() {
    when(brewSessionRepository.findTopRated(any(Pageable.class))).thenReturn(List.of());
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

    recipeStatsService.getTopRated(0);
    recipeStatsService.getTopRated(999);

    verify(brewSessionRepository, org.mockito.Mockito.times(2)).findTopRated(captor.capture());
    assertThat(captor.getAllValues().get(0).getPageSize()).isEqualTo(1);
    assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(20);
    assertThat(captor.getAllValues().get(0)).isEqualTo(PageRequest.of(0, 1));
  }

  @Test
  void getMostBrewed_shouldMapRowsPreservingOrder() {
    when(brewSessionRepository.findMostBrewed(any(Pageable.class)))
        .thenReturn(List.of(mostBrewed(2L, "Busy", 9L), mostBrewed(1L, "Quiet", 3L)));

    List<MostBrewedRecipeResponse> response = recipeStatsService.getMostBrewed(5);

    assertThat(response).hasSize(2);
    assertThat(response.get(0).recipeId()).isEqualTo(2L);
    assertThat(response.get(0).recipeName()).isEqualTo("Busy");
    assertThat(response.get(0).totalSessions()).isEqualTo(9L);
    assertThat(response.get(1).recipeId()).isEqualTo(1L);
  }

  @Test
  void getMostBrewed_shouldClampLimitToRange() {
    when(brewSessionRepository.findMostBrewed(any(Pageable.class))).thenReturn(List.of());
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

    recipeStatsService.getMostBrewed(0);
    recipeStatsService.getMostBrewed(999);

    verify(brewSessionRepository, org.mockito.Mockito.times(2)).findMostBrewed(captor.capture());
    assertThat(captor.getAllValues().get(0)).isEqualTo(PageRequest.of(0, 1));
    assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(20);
  }

  private MostBrewedRecipe mostBrewed(Long id, String name, long total) {
    return new MostBrewedRecipe() {
      @Override
      public Long getRecipeId() {
        return id;
      }

      @Override
      public String getRecipeName() {
        return name;
      }

      @Override
      public long getTotalSessions() {
        return total;
      }
    };
  }

  private TopRatedRecipe topRated(Long id, String name, Double avg, long total) {
    return new TopRatedRecipe() {
      @Override
      public Long getRecipeId() {
        return id;
      }

      @Override
      public String getRecipeName() {
        return name;
      }

      @Override
      public Double getAverageRating() {
        return avg;
      }

      @Override
      public long getTotalSessions() {
        return total;
      }
    };
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
