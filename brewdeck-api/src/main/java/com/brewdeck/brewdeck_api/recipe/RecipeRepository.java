package com.brewdeck.brewdeck_api.recipe;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository
    extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

  @Override
  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findById(Long id);

  @Override
  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findAll(Specification<Recipe> spec, Pageable pageable);

  List<Recipe> findByFavoriteTrue();

  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findByFavoriteTrue(Pageable pageable);

  long countByFavoriteTrue();

  List<Recipe> findByCoffeeId(Long coffeeId);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findByCoffeeId(Long coffeeId, Pageable pageable);

  List<Recipe> findByMethodId(Long methodId);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findByMethodId(Long methodId, Pageable pageable);
}
