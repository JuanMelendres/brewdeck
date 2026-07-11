package com.brewdeck.brewdeck_api.method;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BrewMethodRepository extends JpaRepository<BrewMethod, Long> {

  @Query(
      """
      select m.id as methodId, m.name as methodName, count(r) as recipeCount
      from BrewMethod m
      left join Recipe r on r.method = m and r.owner.id = :ownerId
      group by m.id, m.name
      order by count(r) desc, m.name asc
      """)
  List<MethodUsage> findUsage(Long ownerId);
}
