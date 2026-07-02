package com.brewdeck.brewdeck_api.session;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BrewSessionRepository
    extends JpaRepository<BrewSession, Long>, JpaSpecificationExecutor<BrewSession> {

  List<BrewSession> findByRecipeIdOrderByBrewedAtDesc(Long recipeId);

  Page<BrewSession> findByRecipeIdOrderByBrewedAtDesc(Long recipeId, Pageable pageable);

  @Query("select avg(s.rating) from BrewSession s where s.rating is not null")
  Double findAverageRating();
}
