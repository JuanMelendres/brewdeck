package com.brewdeck.brewdeck_api.session;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brew-sessions")
@RequiredArgsConstructor
public class BrewSessionController {

  private final BrewSessionService brewSessionService;

  @GetMapping
  public ResponseEntity<List<BrewSessionResponse>> findAll() {
    return ResponseEntity.ok(brewSessionService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<BrewSessionResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(brewSessionService.findById(id));
  }

  @GetMapping("/recipe/{recipeId}")
  public ResponseEntity<List<BrewSessionResponse>> findByRecipeId(@PathVariable Long recipeId) {
    return ResponseEntity.ok(brewSessionService.findByRecipeId(recipeId));
  }

  @PostMapping
  public ResponseEntity<BrewSessionResponse> create(
      @Valid @RequestBody BrewSessionRequest request) {
    BrewSessionResponse response = brewSessionService.create(request);

    URI location = URI.create("/api/brew-sessions/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<BrewSessionResponse> update(
      @PathVariable Long id, @Valid @RequestBody BrewSessionRequest request) {
    return ResponseEntity.ok(brewSessionService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    brewSessionService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
