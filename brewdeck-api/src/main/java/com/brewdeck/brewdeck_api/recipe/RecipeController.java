package com.brewdeck.brewdeck_api.recipe;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

  private final RecipeService recipeService;

  @GetMapping
  public ResponseEntity<List<RecipeResponse>> findAll() {
    return ResponseEntity.ok(recipeService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecipeResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.findById(id));
  }

  @GetMapping("/favorites")
  public ResponseEntity<List<RecipeResponse>> findFavorites() {
    return ResponseEntity.ok(recipeService.findFavorites());
  }

  @GetMapping("/coffee/{coffeeId}")
  public ResponseEntity<List<RecipeResponse>> findByCoffeeId(@PathVariable Long coffeeId) {
    return ResponseEntity.ok(recipeService.findByCoffeeId(coffeeId));
  }

  @GetMapping("/method/{methodId}")
  public ResponseEntity<List<RecipeResponse>> findByMethodId(@PathVariable Long methodId) {
    return ResponseEntity.ok(recipeService.findByMethodId(methodId));
  }

  @PostMapping
  public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeRequest request) {
    RecipeResponse response = recipeService.create(request);

    URI location = URI.create("/api/recipes/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<RecipeResponse> update(
      @PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
    return ResponseEntity.ok(recipeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    recipeService.delete(id);

    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/favorite")
  public ResponseEntity<RecipeResponse> markAsFavorite(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.markAsFavorite(id));
  }

  @PatchMapping("/{id}/unfavorite")
  public ResponseEntity<RecipeResponse> removeFromFavorites(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.removeFromFavorites(id));
  }
}
