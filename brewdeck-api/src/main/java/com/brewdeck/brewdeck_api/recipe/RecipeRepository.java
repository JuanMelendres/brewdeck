package com.brewdeck.brewdeck_api.recipe;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
  List<Recipe> findByFavoriteTrue();

  List<Recipe> findByCoffeeId(Long coffeeId);

  List<Recipe> findByMethodId(Long methodId);
}
