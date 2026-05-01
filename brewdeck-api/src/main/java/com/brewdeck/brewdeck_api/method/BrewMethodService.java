package com.brewdeck.brewdeck_api.method;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrewMethodService {

  private final BrewMethodRepository brewMethodRepository;

  public List<BrewMethodResponse> findAll() {
    return brewMethodRepository.findAll().stream().map(BrewMethodResponse::fromEntity).toList();
  }

  public BrewMethodResponse findById(Long id) {
    BrewMethod method =
        brewMethodRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew method not found"));

    return BrewMethodResponse.fromEntity(method);
  }

  public BrewMethodResponse create(BrewMethodRequest request) {
    BrewMethod method =
        BrewMethod.builder().name(request.name()).description(request.description()).build();

    return BrewMethodResponse.fromEntity(brewMethodRepository.save(method));
  }

  public BrewMethodResponse update(Long id, BrewMethodRequest request) {
    BrewMethod method =
        brewMethodRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew method not found"));

    method.setName(request.name());
    method.setDescription(request.description());

    return BrewMethodResponse.fromEntity(brewMethodRepository.save(method));
  }

  public void delete(Long id) {
    if (!brewMethodRepository.existsById(id)) {
      throw new EntityNotFoundException("Brew method not found");
    }

    brewMethodRepository.deleteById(id);
  }
}
