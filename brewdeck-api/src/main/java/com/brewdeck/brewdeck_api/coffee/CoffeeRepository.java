package com.brewdeck.brewdeck_api.coffee;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoffeeRepository
    extends JpaRepository<Coffee, Long>, JpaSpecificationExecutor<Coffee> {

  Optional<Coffee> findByIdAndOwnerId(Long id, Long ownerId);

  boolean existsByIdAndOwnerId(Long id, Long ownerId);

  long countByOwnerId(Long ownerId);

  /** Every row the user owns, for the data export (ADR-020). */
  List<Coffee> findAllByOwnerIdOrderByIdAsc(Long ownerId);

  /** Deletes every row the user owns (account deletion, ADR-020). */
  @Modifying
  @Query("DELETE FROM Coffee c WHERE c.owner.id = :ownerId")
  int deleteAllByOwnerId(@Param("ownerId") Long ownerId);
}
