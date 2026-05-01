package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeService {

  private final RecipeRepository recipeRepository;
  private final CoffeeRepository coffeeRepository;
  private final BrewMethodRepository brewMethodRepository;

  public List<RecipeResponse> findAll() {
    return recipeRepository.findAll().stream().map(RecipeResponse::fromEntity).toList();
  }

  public RecipeResponse findById(Long id) {
    Recipe recipe =
        recipeRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    return RecipeResponse.fromEntity(recipe);
  }

  public List<RecipeResponse> findFavorites() {
    return recipeRepository.findByFavoriteTrue().stream().map(RecipeResponse::fromEntity).toList();
  }

  public List<RecipeResponse> findByCoffeeId(Long coffeeId) {
    return recipeRepository.findByCoffeeId(coffeeId).stream()
        .map(RecipeResponse::fromEntity)
        .toList();
  }

  public List<RecipeResponse> findByMethodId(Long methodId) {
    return recipeRepository.findByMethodId(methodId).stream()
        .map(RecipeResponse::fromEntity)
        .toList();
  }

  public RecipeResponse create(RecipeRequest request) {
    Coffee coffee =
        coffeeRepository
            .findById(request.coffeeId())
            .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));

    BrewMethod method =
        brewMethodRepository
            .findById(request.methodId())
            .orElseThrow(() -> new EntityNotFoundException("Brew method not found"));

    Recipe recipe =
        Recipe.builder()
            .coffee(coffee)
            .method(method)
            .name(request.name())
            .coffeeGrams(request.coffeeGrams())
            .waterGrams(request.waterGrams())
            .ratio(request.ratio())
            .grindSetting(request.grindSetting())
            .waterTemp(request.waterTemp())
            .brewTime(request.brewTime())
            .steps(request.steps())
            .expectedTaste(request.expectedTaste())
            .favorite(Boolean.TRUE.equals(request.favorite()))
            .build();

    return RecipeResponse.fromEntity(recipeRepository.save(recipe));
  }

  public RecipeResponse update(Long id, RecipeRequest request) {
    Recipe recipe =
        recipeRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    Coffee coffee =
        coffeeRepository
            .findById(request.coffeeId())
            .orElseThrow(() -> new EntityNotFoundException("Coffee not found"));

    BrewMethod method =
        brewMethodRepository
            .findById(request.methodId())
            .orElseThrow(() -> new EntityNotFoundException("Brew method not found"));

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

    return RecipeResponse.fromEntity(recipeRepository.save(recipe));
  }

  public void delete(Long id) {
    if (!recipeRepository.existsById(id)) {
      throw new EntityNotFoundException("Recipe not found");
    }

    recipeRepository.deleteById(id);
  }
}
