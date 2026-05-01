package com.brewdeck.brewdeck_api.session;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrewSessionRepository extends JpaRepository<BrewSession, Long> {
  List<BrewSession> findByRecipeIdOrderByBrewedAtDesc(Long recipeId);
}
