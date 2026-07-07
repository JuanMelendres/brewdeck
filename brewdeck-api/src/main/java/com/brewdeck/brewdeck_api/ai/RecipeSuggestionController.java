package com.brewdeck.brewdeck_api.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@Tag(name = "AI Recipe Suggestions", description = "AI-generated brewing parameters")
public class RecipeSuggestionController {

  private final RecipeSuggestionService recipeSuggestionService;

  @PostMapping("/suggest")
  @Operation(
      summary = "Suggest brewing parameters",
      description =
          "Generates AI brewing parameters for a coffee and brew method. The result is not"
              + " persisted; the client uses it to pre-fill the recipe form.")
  public ResponseEntity<SuggestedRecipeResponse> suggest(
      @Valid @RequestBody SuggestRecipeRequest request) {
    return ResponseEntity.ok(recipeSuggestionService.suggest(request));
  }
}
