package com.brewdeck.brewdeck_api.coffee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CoffeeRepository
    extends JpaRepository<Coffee, Long>, JpaSpecificationExecutor<Coffee> {}
