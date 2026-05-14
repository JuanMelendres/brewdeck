package com.brewdeck.brewdeck_api.session;

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
@RequestMapping("/api/brew-sessions")
@RequiredArgsConstructor
public class BrewSessionController {

  private final BrewSessionService brewSessionService;

  @GetMapping
  public ResponseEntity<PageResponse<BrewSessionResponse>> findAll(
      @ModelAttribute BrewSessionFilter filter,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(brewSessionService.search(filter, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BrewSessionResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(brewSessionService.findById(id));
  }

  @GetMapping("/recipe/{recipeId}")
  public ResponseEntity<PageResponse<BrewSessionResponse>> findByRecipeId(
      @PathVariable Long recipeId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
    return ResponseEntity.ok(brewSessionService.findByRecipeId(recipeId, pageable));
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
