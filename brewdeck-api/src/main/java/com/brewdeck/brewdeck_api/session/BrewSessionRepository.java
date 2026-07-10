package com.brewdeck.brewdeck_api.session;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BrewSessionRepository
    extends JpaRepository<BrewSession, Long>, JpaSpecificationExecutor<BrewSession> {

  @Override
  @EntityGraph(attributePaths = "recipe")
  Optional<BrewSession> findById(Long id);

  @Override
  @EntityGraph(attributePaths = "recipe")
  Page<BrewSession> findAll(Specification<BrewSession> spec, Pageable pageable);

  @EntityGraph(attributePaths = "recipe")
  Optional<BrewSession> findByIdAndOwnerId(Long id, Long ownerId);

  boolean existsByIdAndOwnerId(Long id, Long ownerId);

  long countByOwnerId(Long ownerId);

  List<BrewSession> findByRecipeIdOrderByBrewedAtDesc(Long recipeId);

  List<BrewSession> findTop10ByRecipeIdAndRatingIsNotNullOrderByBrewedAtDesc(Long recipeId);

  @EntityGraph(attributePaths = "recipe")
  Page<BrewSession> findByRecipeIdOrderByBrewedAtDesc(Long recipeId, Pageable pageable);

  @EntityGraph(attributePaths = "recipe")
  Page<BrewSession> findByRecipeIdAndOwnerIdOrderByBrewedAtDesc(
      Long recipeId, Long ownerId, Pageable pageable);

  @Query("select avg(s.rating) from BrewSession s where s.rating is not null")
  Double findAverageRating();

  @Query(
      """
      select count(s) as totalSessions,
             avg(s.rating) as averageRating,
             max(s.brewedAt) as lastBrewedAt
      from BrewSession s
      where s.recipe.id = :recipeId
      """)
  RecipeSessionStats findStatsByRecipeId(Long recipeId);

  @Query(
      """
      select s.recipe.id as recipeId,
             s.recipe.name as recipeName,
             avg(s.rating) as averageRating,
             count(s) as totalSessions
      from BrewSession s
      where s.rating is not null
      group by s.recipe.id, s.recipe.name
      order by avg(s.rating) desc
      """)
  List<TopRatedRecipe> findTopRated(Pageable pageable);

  @Query(
      """
      select s.recipe.id as recipeId,
             s.recipe.name as recipeName,
             count(s) as totalSessions
      from BrewSession s
      group by s.recipe.id, s.recipe.name
      order by count(s) desc
      """)
  List<MostBrewedRecipe> findMostBrewed(Pageable pageable);
}
