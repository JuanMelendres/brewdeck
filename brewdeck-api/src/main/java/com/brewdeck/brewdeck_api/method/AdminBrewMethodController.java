package com.brewdeck.brewdeck_api.method;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin-only management of the shared brew-method catalog. Guarded by {@code /api/admin/**}. */
@RestController
@RequestMapping("/api/admin/brew-methods")
@RequiredArgsConstructor
@Tag(name = "Admin - Brew Methods", description = "Manage the shared brew-method catalog")
public class AdminBrewMethodController {

  private final BrewMethodService brewMethodService;

  @PostMapping
  @Operation(summary = "Create a shared brew method")
  public ResponseEntity<BrewMethodResponse> create(@Valid @RequestBody BrewMethodRequest request) {
    BrewMethodResponse response = brewMethodService.createShared(request);

    URI location = URI.create("/api/brew-methods/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a shared brew method")
  public ResponseEntity<BrewMethodResponse> update(
      @PathVariable Long id, @Valid @RequestBody BrewMethodRequest request) {
    return ResponseEntity.ok(brewMethodService.updateShared(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a shared brew method")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    brewMethodService.deleteShared(id);

    return ResponseEntity.noContent().build();
  }
}
