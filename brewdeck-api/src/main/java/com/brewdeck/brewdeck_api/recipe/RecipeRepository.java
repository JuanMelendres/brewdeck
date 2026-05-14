package com.brewdeck.brewdeck_api.recipe;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository
    extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

  List<Recipe> findByFavoriteTrue();

  Page<Recipe> findByFavoriteTrue(Pageable pageable);

  List<Recipe> findByCoffeeId(Long coffeeId);

  Page<Recipe> findByCoffeeId(Long coffeeId, Pageable pageable);

  List<Recipe> findByMethodId(Long methodId);

  Page<Recipe> findByMethodId(Long methodId, Pageable pageable);
}
