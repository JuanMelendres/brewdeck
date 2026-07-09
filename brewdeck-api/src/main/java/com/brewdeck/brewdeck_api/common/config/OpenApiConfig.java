package com.brewdeck.brewdeck_api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

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
                .license(new License().name("Apache 2.0")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
