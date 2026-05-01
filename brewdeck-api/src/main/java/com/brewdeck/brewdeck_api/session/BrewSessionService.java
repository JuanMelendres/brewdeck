package com.brewdeck.brewdeck_api.session;

import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrewSessionService {

  private final BrewSessionRepository brewSessionRepository;
  private final RecipeRepository recipeRepository;

  public List<BrewSessionResponse> findAll() {
    return brewSessionRepository.findAll().stream().map(BrewSessionResponse::fromEntity).toList();
  }

  public BrewSessionResponse findById(Long id) {
    BrewSession session =
        brewSessionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew session not found"));

    return BrewSessionResponse.fromEntity(session);
  }

  public List<BrewSessionResponse> findByRecipeId(Long recipeId) {
    return brewSessionRepository.findByRecipeIdOrderByBrewedAtDesc(recipeId).stream()
        .map(BrewSessionResponse::fromEntity)
        .toList();
  }

  public BrewSessionResponse create(BrewSessionRequest request) {
    Recipe recipe =
        recipeRepository
            .findById(request.recipeId())
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    BrewSession session =
        BrewSession.builder()
            .recipe(recipe)
            .actualGrind(request.actualGrind())
            .actualTemp(request.actualTemp())
            .actualTime(request.actualTime())
            .tasteResult(request.tasteResult())
            .rating(request.rating())
            .adjustmentNotes(request.adjustmentNotes())
            .build();

    return BrewSessionResponse.fromEntity(brewSessionRepository.save(session));
  }

  public BrewSessionResponse update(Long id, BrewSessionRequest request) {
    BrewSession session =
        brewSessionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew session not found"));

    Recipe recipe =
        recipeRepository
            .findById(request.recipeId())
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    session.setRecipe(recipe);
    session.setActualGrind(request.actualGrind());
    session.setActualTemp(request.actualTemp());
    session.setActualTime(request.actualTime());
    session.setTasteResult(request.tasteResult());
    session.setRating(request.rating());
    session.setAdjustmentNotes(request.adjustmentNotes());

    return BrewSessionResponse.fromEntity(brewSessionRepository.save(session));
  }

  public void delete(Long id) {
    if (!brewSessionRepository.existsById(id)) {
      throw new EntityNotFoundException("Brew session not found");
    }

    brewSessionRepository.deleteById(id);
  }
}
