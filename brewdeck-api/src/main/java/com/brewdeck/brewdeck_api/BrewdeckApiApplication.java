package com.brewdeck.brewdeck_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BrewdeckApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(BrewdeckApiApplication.class, args);
  }
}
