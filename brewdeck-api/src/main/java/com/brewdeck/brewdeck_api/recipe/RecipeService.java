package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
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
public class RecipeService {

  private static final String RECIPE_NOT_FOUND = "Recipe not found";
  private static final String COFFEE_NOT_FOUND = "Coffee not found";
  private static final String BREW_METHOD_NOT_FOUND = "Brew method not found";
  private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

  private final RecipeRepository recipeRepository;
  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;
  private final CurrentUserProvider currentUserProvider;

  public PageResponse<RecipeResponse> search(RecipeFilter filter, Pageable pageable) {
    return PageResponse.fromPage(
        recipeRepository
            .findAll(
                RecipeSpecification.hasCoffeeId(filter.coffeeId())
                    .and(RecipeSpecification.hasMethodId(filter.methodId()))
                    .and(RecipeSpecification.isFavorite(filter.favorite()))
                    .and(RecipeSpecification.nameContains(filter.name())),
                pageable)
            .map(RecipeResponse::fromEntity));
  }

  public RecipeResponse findById(Long id) {
    return RecipeResponse.fromEntity(findRecipeById(id));
  }

  public PageResponse<RecipeResponse> findFavorites(Pageable pageable) {
    return PageResponse.fromPage(
        recipeRepository.findByFavoriteTrue(pageable).map(RecipeResponse::fromEntity));
  }

  public PageResponse<RecipeResponse> findByCoffeeId(Long coffeeId, Pageable pageable) {
    return PageResponse.fromPage(
        recipeRepository.findByCoffeeId(coffeeId, pageable).map(RecipeResponse::fromEntity));
  }

  public PageResponse<RecipeResponse> findByMethodId(Long methodId, Pageable pageable) {
    return PageResponse.fromPage(
        recipeRepository.findByMethodId(methodId, pageable).map(RecipeResponse::fromEntity));
  }

  @Transactional
  public RecipeResponse create(RecipeRequest request) {
    Coffee coffee = findCoffeeById(request.coffeeId());
    BrewMethod method = findBrewMethodById(request.methodId());

    Recipe recipe = new Recipe();
    recipe.setOwner(currentUserProvider.require());
    applyRequest(recipe, request, coffee, method);

    Recipe saved = recipeRepository.save(recipe);
    log.info("Created recipe id={}", saved.getId());

    return RecipeResponse.fromEntity(saved);
  }

  @Transactional
  public RecipeResponse update(Long id, RecipeRequest request) {
    Recipe recipe = findRecipeById(id);
    Coffee coffee = findCoffeeById(request.coffeeId());
    BrewMethod method = findBrewMethodById(request.methodId());

    applyRequest(recipe, request, coffee, method);

    Recipe saved = recipeRepository.save(recipe);
    log.info("Updated recipe id={}", saved.getId());

    return RecipeResponse.fromEntity(saved);
  }

  @Transactional
  public void delete(Long id) {
    if (!recipeRepository.existsById(id)) {
      throw new EntityNotFoundException(RECIPE_NOT_FOUND);
    }

    recipeRepository.deleteById(id);
    log.info("Deleted recipe id={}", id);
  }

  @Transactional
  public RecipeResponse markAsFavorite(Long id) {
    Recipe recipe = findRecipeById(id);

    recipe.setFavorite(true);

    Recipe saved = recipeRepository.save(recipe);
    log.info("Marked recipe id={} as favorite", saved.getId());

    return RecipeResponse.fromEntity(saved);
  }

  @Transactional
  public RecipeResponse removeFromFavorites(Long id) {
    Recipe recipe = findRecipeById(id);

    recipe.setFavorite(false);

    Recipe saved = recipeRepository.save(recipe);
    log.info("Removed recipe id={} from favorites", saved.getId());

    return RecipeResponse.fromEntity(saved);
  }

  private Recipe findRecipeById(Long id) {
    return recipeRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(RECIPE_NOT_FOUND));
  }

  private Coffee findCoffeeById(Long id) {
    return coffeeRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(COFFEE_NOT_FOUND));
  }

  private BrewMethod findBrewMethodById(Long id) {
    return brewMethodRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(BREW_METHOD_NOT_FOUND));
  }

  private void applyRequest(
      Recipe recipe, RecipeRequest request, Coffee coffee, BrewMethod method) {
    recipe.setCoffee(coffee);
    recipe.setMethod(method);
    recipe.setName(request.name());
    recipe.setCoffeeGrams(request.coffeeGrams());
    recipe.setWaterGrams(request.waterGrams());
    recipe.setRatio(request.ratio());
    recipe.setGrindSetting(request.grindSetting());
    recipe.setWaterTemp(request.waterTemp());
    recipe.setBrewTime(request.brewTime());
    recipe.setSteps(request.steps());
    recipe.setExpectedTaste(request.expectedTaste());
    recipe.setFavorite(Boolean.TRUE.equals(request.favorite()));
  }

  @Transactional
  public RecipeResponse share(Long id) {
    Recipe recipe = findRecipeById(id);
    if (recipe.getShareToken() == null) {
      recipe.setShareToken(generateToken());
      recipe = recipeRepository.save(recipe);
      log.info("Shared recipe id={}", recipe.getId());
    }
    return RecipeResponse.fromEntity(recipe);
  }

  @Transactional
  public RecipeResponse unshare(Long id) {
    Recipe recipe = findRecipeById(id);
    recipe.setShareToken(null);
    Recipe saved = recipeRepository.save(recipe);
    log.info("Unshared recipe id={}", saved.getId());
    return RecipeResponse.fromEntity(saved);
  }

  public PublicRecipeResponse getByShareToken(String token) {
    return recipeRepository
        .findByShareToken(token)
        .map(PublicRecipeResponse::fromEntity)
        .orElseThrow(() -> new EntityNotFoundException(RECIPE_NOT_FOUND));
  }

  private String generateToken() {
    byte[] bytes = new byte[16];
    SECURE_RANDOM.nextBytes(bytes);
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
