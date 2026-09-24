package com.brewdeck.brewdeck_api.method;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrewMethodRepository extends JpaRepository<BrewMethod, Long> {

  /** Methods the user can see: the shared catalog plus their own private methods. */
  @Query("select m from BrewMethod m where m.owner is null or m.owner.id = :ownerId")
  Page<BrewMethod> findVisibleTo(@Param("ownerId") Long ownerId, Pageable pageable);

  @Query(
      "select m from BrewMethod m where m.id = :id and (m.owner is null or m.owner.id = :ownerId)")
  Optional<BrewMethod> findVisibleById(@Param("id") Long id, @Param("ownerId") Long ownerId);

  @Query("select count(m) from BrewMethod m where m.owner is null or m.owner.id = :ownerId")
  long countVisibleTo(@Param("ownerId") Long ownerId);

  /** Shared-catalog methods only (no owner) — the admin's scope. */
  Optional<BrewMethod> findByIdAndOwnerIsNull(Long id);

  @Query(
      """
      select m.id as methodId, m.name as methodName, count(r) as recipeCount
      from BrewMethod m
      left join Recipe r on r.method = m and r.owner.id = :ownerId
      where m.owner is null or m.owner.id = :ownerId
      group by m.id, m.name
      order by count(r) desc, m.name asc
      """)
  List<MethodUsage> findUsage(@Param("ownerId") Long ownerId);
}
