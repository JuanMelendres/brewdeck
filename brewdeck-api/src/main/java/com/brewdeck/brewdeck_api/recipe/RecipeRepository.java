package com.brewdeck.brewdeck_api.recipe;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository
    extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

  List<Recipe> findByFavoriteTrue();

  List<Recipe> findByCoffeeId(Long coffeeId);

  List<Recipe> findByMethodId(Long methodId);
}
