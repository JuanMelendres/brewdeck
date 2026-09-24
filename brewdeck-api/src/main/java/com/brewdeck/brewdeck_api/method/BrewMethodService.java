package com.brewdeck.brewdeck_api.method;

import com.brewdeck.brewdeck_api.auth.CurrentUserProvider;
import com.brewdeck.brewdeck_api.common.pagination.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brew methods come in two tiers: a shared catalog every user reads (owner is null, managed by
 * admins through {@code /api/admin/brew-methods}) and private methods each user creates for
 * themselves. A user sees the catalog plus their own methods; another user's private methods are
 * invisible (404), and the catalog is read-only to them (403).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BrewMethodService {

  private static final String BREW_METHOD_NOT_FOUND = "Brew method not found";

  private final BrewMethodRepository brewMethodRepository;
  private final CurrentUserProvider currentUserProvider;

  public PageResponse<BrewMethodResponse> findAll(Pageable pageable) {
    return PageResponse.fromPage(
        brewMethodRepository
            .findVisibleTo(currentOwnerId(), pageable)
            .map(BrewMethodResponse::fromEntity));
  }

  public List<MethodUsageResponse> getUsage() {
    return brewMethodRepository.findUsage(currentOwnerId()).stream()
        .map(
            usage ->
                new MethodUsageResponse(
                    usage.getMethodId(), usage.getMethodName(), usage.getRecipeCount()))
        .toList();
  }

  public BrewMethodResponse findById(Long id) {
    return BrewMethodResponse.fromEntity(findVisible(id));
  }

  /** Creates a private method owned by the current user. */
  @Transactional
  public BrewMethodResponse create(BrewMethodRequest request) {
    BrewMethod method =
        BrewMethod.builder()
            .owner(currentUserProvider.require())
            .name(request.name())
            .description(request.description())
            .build();

    BrewMethod saved = brewMethodRepository.save(method);
    log.info("Created private brew method id={}", saved.getId());

    return BrewMethodResponse.fromEntity(saved);
  }

  @Transactional
  public BrewMethodResponse update(Long id, BrewMethodRequest request) {
    BrewMethod method = findOwnedByCurrentUser(id);

    method.setName(request.name());
    method.setDescription(request.description());

    BrewMethod saved = brewMethodRepository.save(method);
    log.info("Updated private brew method id={}", saved.getId());

    return BrewMethodResponse.fromEntity(saved);
  }

  @Transactional
  public void delete(Long id) {
    BrewMethod method = findOwnedByCurrentUser(id);

    brewMethodRepository.delete(method);
    log.info("Deleted private brew method id={}", id);
  }

  /** Creates a method in the shared catalog. Admin only (enforced on {@code /api/admin/**}). */
  @Transactional
  public BrewMethodResponse createShared(BrewMethodRequest request) {
    BrewMethod method =
        BrewMethod.builder().name(request.name()).description(request.description()).build();

    BrewMethod saved = brewMethodRepository.save(method);
    log.info("Created shared brew method id={}", saved.getId());

    return BrewMethodResponse.fromEntity(saved);
  }

  @Transactional
  public BrewMethodResponse updateShared(Long id, BrewMethodRequest request) {
    BrewMethod method = findShared(id);

    method.setName(request.name());
    method.setDescription(request.description());

    BrewMethod saved = brewMethodRepository.save(method);
    log.info("Updated shared brew method id={}", saved.getId());

    return BrewMethodResponse.fromEntity(saved);
  }

  @Transactional
  public void deleteShared(Long id) {
    BrewMethod method = findShared(id);

    brewMethodRepository.delete(method);
    log.info("Deleted shared brew method id={}", id);
  }

  private BrewMethod findVisible(Long id) {
    return brewMethodRepository
        .findVisibleById(id, currentOwnerId())
        .orElseThrow(() -> new EntityNotFoundException(BREW_METHOD_NOT_FOUND));
  }

  private BrewMethod findOwnedByCurrentUser(Long id) {
    BrewMethod method = findVisible(id);
    if (method.isShared()) {
      throw new AccessDeniedException("Shared brew methods can only be changed by an admin");
    }
    return method;
  }

  private BrewMethod findShared(Long id) {
    return brewMethodRepository
        .findByIdAndOwnerIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException(BREW_METHOD_NOT_FOUND));
  }

  private Long currentOwnerId() {
    return currentUserProvider.require().getId();
  }
}
