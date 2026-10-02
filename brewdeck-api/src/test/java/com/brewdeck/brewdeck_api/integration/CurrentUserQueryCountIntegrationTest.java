package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.Locale;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * The JWT filter loads the user once per request; nothing afterwards may load it again (audit
 * finding 14). Before the fix, creating or updating a recipe looked the user up 4 times.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CurrentUserQueryCountIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private BrewMethodRepository brewMethodRepository;

  private Statistics statistics;
  private String bearer;

  @BeforeEach
  void registerUser() throws Exception {
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    String response =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"query-count-"
                            + System.nanoTime()
                            + "@example.com\",\"password\":\"password123\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    bearer = "Bearer " + JsonPath.read(response, "$.token");
  }

  /** Executions of JPQL queries that read the users table. */
  private long userLookups() {
    return Arrays.stream(statistics.getQueries())
        .filter(query -> query.toLowerCase(Locale.ROOT).contains("from user "))
        .mapToLong(query -> statistics.getQueryStatistics(query).getExecutionCount())
        .sum();
  }

  private long userLookupsDuring(RequestBuilder request) throws Exception {
    statistics.clear();
    mockMvc.perform(request).andExpect(status().is2xxSuccessful());
    return userLookups();
  }

  @Test
  void writeAndReadRequests_resolveTheUserExactlyOnce() throws Exception {
    Number coffeeId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/coffees")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Query count coffee\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    BrewMethod method =
        brewMethodRepository.save(
            BrewMethod.builder().name("Query count method " + System.nanoTime()).build());
    String recipeBody =
        "{\"coffeeId\":" + coffeeId + ",\"methodId\":" + method.getId() + ",\"name\":\"R\"}";

    statistics.clear();
    String created =
        mockMvc
            .perform(
                post("/api/recipes")
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(recipeBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(userLookups()).as("POST /api/recipes").isEqualTo(1);
    Number recipeId = JsonPath.read(created, "$.id");

    assertThat(
            userLookupsDuring(
                put("/api/recipes/{id}", recipeId.longValue())
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(recipeBody)))
        .as("PUT /api/recipes/{id}")
        .isEqualTo(1);
    assertThat(
            userLookupsDuring(
                get("/api/recipes/{id}", recipeId.longValue()).header("Authorization", bearer)))
        .as("GET /api/recipes/{id}")
        .isEqualTo(1);
    assertThat(userLookupsDuring(get("/api/dashboard/summary").header("Authorization", bearer)))
        .as("GET /api/dashboard/summary")
        .isEqualTo(1);
  }
}
