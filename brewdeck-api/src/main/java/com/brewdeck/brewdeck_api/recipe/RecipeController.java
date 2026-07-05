package com.brewdeck.brewdeck_api.recipe;

import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Recipes", description = "Manage recipes and favorites")
public class RecipeController {

  private final RecipeService recipeService;
  private final RecipeStatsService recipeStatsService;

  @GetMapping
  @Operation(summary = "List recipes", description = "Returns a paginated list of recipes.")
  public ResponseEntity<PageResponse<RecipeResponse>> findAll(
      @ModelAttribute RecipeFilter filter,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.search(filter, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get recipe by id")
  public ResponseEntity<RecipeResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.findById(id));
  }

  @GetMapping("/{id}/stats")
  @Operation(
      summary = "Get recipe brew statistics",
      description =
          "Returns total brew sessions, average rating, and last brewed timestamp for a recipe.")
  public ResponseEntity<RecipeStatsResponse> getStats(@PathVariable Long id) {
    return ResponseEntity.ok(recipeStatsService.getStats(id));
  }

  @GetMapping("/favorites")
  @Operation(summary = "List favorite recipes")
  public ResponseEntity<PageResponse<RecipeResponse>> findFavorites(
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.findFavorites(pageable));
  }

  @GetMapping("/coffee/{coffeeId}")
  @Operation(summary = "List recipes by coffee")
  public ResponseEntity<PageResponse<RecipeResponse>> findByCoffeeId(
      @PathVariable Long coffeeId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.findByCoffeeId(coffeeId, pageable));
  }

  @GetMapping("/method/{methodId}")
  @Operation(summary = "List recipes by brew method")
  public ResponseEntity<PageResponse<RecipeResponse>> findByMethodId(
      @PathVariable Long methodId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(recipeService.findByMethodId(methodId, pageable));
  }

  @PostMapping
  @Operation(summary = "Create recipe")
  public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeRequest request) {
    RecipeResponse response = recipeService.create(request);

    URI location = URI.create("/api/recipes/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update recipe")
  public ResponseEntity<RecipeResponse> update(
      @PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
    return ResponseEntity.ok(recipeService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete recipe")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    recipeService.delete(id);

    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/favorite")
  @Operation(summary = "Mark recipe as favorite")
  public ResponseEntity<RecipeResponse> markAsFavorite(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.markAsFavorite(id));
  }

  @PatchMapping("/{id}/unfavorite")
  @Operation(summary = "Remove recipe from favorites")
  public ResponseEntity<RecipeResponse> removeFromFavorites(@PathVariable Long id) {
    return ResponseEntity.ok(recipeService.removeFromFavorites(id));
  }
}
