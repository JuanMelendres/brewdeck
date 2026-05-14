package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

  private final RecipeService recipeService;

  @GetMapping
  public ResponseEntity<PageResponse<RecipeResponse>> findAll(
      @ModelAttribute RecipeFilter filter,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return ResponseEntity.ok(recipeService.search(filter, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecipeResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.findById(id));
  }

  @GetMapping("/favorites")
  public ResponseEntity<PageResponse<RecipeResponse>> findFavorites(
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.findFavorites(pageable));
  }

  @GetMapping("/coffee/{coffeeId}")
  public ResponseEntity<PageResponse<RecipeResponse>> findByCoffeeId(
      @PathVariable Long coffeeId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.findByCoffeeId(coffeeId, pageable));
  }

  @GetMapping("/method/{methodId}")
  public ResponseEntity<PageResponse<RecipeResponse>> findByMethodId(
      @PathVariable Long methodId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.findByMethodId(methodId, pageable));
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
