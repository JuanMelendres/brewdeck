package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.coffee.MostUsedCoffee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findByIdAndOwnerId(Long id, Long ownerId);

  boolean existsByIdAndOwnerId(Long id, Long ownerId);

  long countByOwnerId(Long ownerId);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findByFavoriteTrueAndOwnerId(Long ownerId, Pageable pageable);

  long countByFavoriteTrueAndOwnerId(Long ownerId);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findByCoffeeIdAndOwnerId(Long coffeeId, Long ownerId, Pageable pageable);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findByMethodIdAndOwnerId(Long methodId, Long ownerId, Pageable pageable);

  @Query(
      """
      select r.coffee.id as coffeeId, r.coffee.name as coffeeName, count(r) as recipeCount
      from Recipe r
      group by r.coffee.id, r.coffee.name
      order by count(r) desc, r.coffee.name asc
      """)
  List<MostUsedCoffee> findMostUsedCoffees(Pageable pageable);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findByShareToken(String shareToken);
}
