package com.brewdeck.brewdeck_api.session;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BrewSessionRepository
    extends JpaRepository<BrewSession, Long>, JpaSpecificationExecutor<BrewSession> {

  List<BrewSession> findByRecipeIdOrderByBrewedAtDesc(Long recipeId);
}
