package com.brewdeck.brewdeck_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Authentication is JWT against the users table (ADR-005); nothing uses a UserDetailsService.
// Without the exclusion, Spring Boot creates an in-memory "user" and logs its generated password
// on every start (backlog OPS-10).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class BrewdeckApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(BrewdeckApiApplication.class, args);
  }
}
