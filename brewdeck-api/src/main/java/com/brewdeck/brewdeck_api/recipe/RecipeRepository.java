package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.coffee.MostUsedCoffee;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository
    extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

  @Override
  @NonNull
  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findById(@NonNull Long id);

  @Override
  @NonNull
  @EntityGraph(attributePaths = {"coffee", "method"})
  Page<Recipe> findAll(@Nullable Specification<Recipe> spec, @NonNull Pageable pageable);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findByIdAndOwnerId(Long id, Long ownerId);

  boolean existsByIdAndOwnerId(Long id, Long ownerId);

  long countByOwnerId(Long ownerId);

  /** Recipes referencing a coffee (all belong to the coffee's owner). Guards coffee deletes. */
  long countByCoffeeId(Long coffeeId);

  /** Recipes, of any user, referencing a brew method. Guards brew-method deletes. */
  long countByMethodId(Long methodId);

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
      where r.owner.id = :ownerId
      group by r.coffee.id, r.coffee.name
      order by count(r) desc, r.coffee.name asc, r.coffee.id asc
      """)
  List<MostUsedCoffee> findMostUsedCoffees(@Param("ownerId") Long ownerId, Pageable pageable);

  @EntityGraph(attributePaths = {"coffee", "method"})
  Optional<Recipe> findByShareToken(String shareToken);

  /** Every row the user owns, for the data export (ADR-020). */
  @EntityGraph(attributePaths = {"coffee", "method"})
  List<Recipe> findAllByOwnerIdOrderByIdAsc(Long ownerId);

  /** Deletes every row the user owns (account deletion, ADR-020). */
  @Modifying
  @Query("DELETE FROM Recipe r WHERE r.owner.id = :ownerId")
  int deleteAllByOwnerId(@Param("ownerId") Long ownerId);
}
