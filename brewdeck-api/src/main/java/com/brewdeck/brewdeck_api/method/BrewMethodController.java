package com.brewdeck.brewdeck_api.method;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brew-methods")
@RequiredArgsConstructor
public class BrewMethodController {

  private final BrewMethodService brewMethodService;

  @GetMapping
  public List<BrewMethodResponse> findAll() {
    return brewMethodService.findAll();
  }

  @GetMapping("/{id}")
  public BrewMethodResponse findById(@PathVariable Long id) {
    return brewMethodService.findById(id);
  }

  @PostMapping
  public BrewMethodResponse create(@Valid @RequestBody BrewMethodRequest request) {
    return brewMethodService.create(request);
  }

  @PutMapping("/{id}")
  public BrewMethodResponse update(
      @PathVariable Long id, @Valid @RequestBody BrewMethodRequest request) {
    return brewMethodService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    brewMethodService.delete(id);
  }
}
