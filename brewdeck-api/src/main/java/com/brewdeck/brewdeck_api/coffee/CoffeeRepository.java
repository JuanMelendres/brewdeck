package com.brewdeck.brewdeck_api.coffee;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CoffeeRepository
    extends JpaRepository<Coffee, Long>, JpaSpecificationExecutor<Coffee> {

  Optional<Coffee> findByIdAndOwnerId(Long id, Long ownerId);

  boolean existsByIdAndOwnerId(Long id, Long ownerId);

  long countByOwnerId(Long ownerId);
}
