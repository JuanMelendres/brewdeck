package com.brewdeck.brewdeck_api.session;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BrewSessionService {

  private final BrewSessionRepository brewSessionRepository;
  private final RecipeRepository recipeRepository;
  private final CurrentUserProvider currentUserProvider;

  public PageResponse<BrewSessionResponse> search(BrewSessionFilter filter, Pageable pageable) {
    return PageResponse.fromPage(
        brewSessionRepository
            .findAll(
                BrewSessionSpecification.hasRecipeId(filter.recipeId())
                    .and(BrewSessionSpecification.hasRating(filter.rating())),
                pageable)
            .map(BrewSessionResponse::fromEntity));
  }

  public BrewSessionResponse findById(Long id) {
    BrewSession session =
        brewSessionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew session not found"));

    return BrewSessionResponse.fromEntity(session);
  }

  public PageResponse<BrewSessionResponse> findByRecipeId(Long recipeId, Pageable pageable) {
    return PageResponse.fromPage(
        brewSessionRepository
            .findByRecipeIdOrderByBrewedAtDesc(recipeId, pageable)
            .map(BrewSessionResponse::fromEntity));
  }

  @Transactional
  public BrewSessionResponse create(BrewSessionRequest request) {
    Recipe recipe =
        recipeRepository
            .findById(request.recipeId())
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    BrewSession session = new BrewSession();
    session.setOwner(currentUserProvider.require());
    applyRequest(session, request, recipe);

    BrewSession saved = brewSessionRepository.save(session);
    log.info("Created brew session id={} recipeId={}", saved.getId(), recipe.getId());

    return BrewSessionResponse.fromEntity(saved);
  }

  @Transactional
  public BrewSessionResponse update(Long id, BrewSessionRequest request) {
    BrewSession session =
        brewSessionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew session not found"));

    Recipe recipe =
        recipeRepository
            .findById(request.recipeId())
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    applyRequest(session, request, recipe);

    BrewSession saved = brewSessionRepository.save(session);
    log.info("Updated brew session id={}", saved.getId());

    return BrewSessionResponse.fromEntity(saved);
  }

  @Transactional
  public void delete(Long id) {
    if (!brewSessionRepository.existsById(id)) {
      throw new EntityNotFoundException("Brew session not found");
    }

    brewSessionRepository.deleteById(id);
    log.info("Deleted brew session id={}", id);
  }

  private void applyRequest(BrewSession session, BrewSessionRequest request, Recipe recipe) {
    session.setRecipe(recipe);
    session.setActualGrind(request.actualGrind());
    session.setActualTemp(request.actualTemp());
    session.setActualTime(request.actualTime());
    session.setTasteResult(request.tasteResult());
    session.setRating(request.rating());
    session.setAdjustmentNotes(request.adjustmentNotes());
  }
}
