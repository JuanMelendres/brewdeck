package com.brewdeck.brewdeck_api.recipe;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

  private final RecipeService recipeService;

  @GetMapping
  public List<RecipeResponse> findAll() {
    return recipeService.findAll();
  }

  @GetMapping("/{id}")
  public RecipeResponse findById(@PathVariable Long id) {
    return recipeService.findById(id);
  }

  @GetMapping("/favorites")
  public List<RecipeResponse> findFavorites() {
    return recipeService.findFavorites();
  }

  @GetMapping("/coffee/{coffeeId}")
  public List<RecipeResponse> findByCoffeeId(@PathVariable Long coffeeId) {
    return recipeService.findByCoffeeId(coffeeId);
  }

  @GetMapping("/method/{methodId}")
  public List<RecipeResponse> findByMethodId(@PathVariable Long methodId) {
    return recipeService.findByMethodId(methodId);
  }

  @PostMapping
  public RecipeResponse create(@Valid @RequestBody RecipeRequest request) {
    return recipeService.create(request);
  }

  @PutMapping("/{id}")
  public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
    return recipeService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    recipeService.delete(id);
  }
}
