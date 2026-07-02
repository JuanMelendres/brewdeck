package com.brewdeck.brewdeck_api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI brewDeckOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("BrewDeck API")
                .description(
                    "REST API for managing coffees, brew methods, recipes and brew sessions.")
                .version("v1")
                .contact(new Contact().name("BrewDeck"))
                .license(new License().name("Apache 2.0")));
  }
}
