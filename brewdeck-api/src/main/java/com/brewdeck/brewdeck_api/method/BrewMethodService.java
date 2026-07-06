package com.brewdeck.brewdeck_api.method;

import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrewMethodService {

  private final BrewMethodRepository brewMethodRepository;

  public PageResponse<BrewMethodResponse> findAll(Pageable pageable) {
    return PageResponse.fromPage(
        brewMethodRepository.findAll(pageable).map(BrewMethodResponse::fromEntity));
  }

  public List<MethodUsageResponse> getUsage() {
    return brewMethodRepository.findUsage().stream()
        .map(
            usage ->
                new MethodUsageResponse(
                    usage.getMethodId(), usage.getMethodName(), usage.getRecipeCount()))
        .toList();
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

    BrewMethod saved = brewMethodRepository.save(method);
    log.info("Created brew method id={}", saved.getId());

    return BrewMethodResponse.fromEntity(saved);
  }

  public BrewMethodResponse update(Long id, BrewMethodRequest request) {
    BrewMethod method =
        brewMethodRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Brew method not found"));

    method.setName(request.name());
    method.setDescription(request.description());

    BrewMethod saved = brewMethodRepository.save(method);
    log.info("Updated brew method id={}", saved.getId());

    return BrewMethodResponse.fromEntity(saved);
  }

  public void delete(Long id) {
    if (!brewMethodRepository.existsById(id)) {
      throw new EntityNotFoundException("Brew method not found");
    }

    brewMethodRepository.deleteById(id);
    log.info("Deleted brew method id={}", id);
  }
}
