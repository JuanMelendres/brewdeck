package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.Role;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Brew methods are a shared catalog: any authenticated user reads them, only ADMIN writes them.
 * Uses real JWTs so the role travels through {@code JwtAuthenticationFilter}, not a mock principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrewMethodAuthorizationIntegrationTest extends PostgresIntegrationTest {

  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;

  @Test
  void regularUser_canReadButCannotWriteBrewMethods() throws Exception {
    String token = registerAndGetToken("method-user-" + System.nanoTime() + "@example.com");
    Long methodId = seedMethod();

    mockMvc
        .perform(get("/api/brew-methods").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/brew-methods/{id}", methodId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/brew-methods")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("User Method")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value("Insufficient permissions"));
    mockMvc
        .perform(
            put("/api/brew-methods/{id}", methodId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Hijacked")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            delete("/api/brew-methods/{id}", methodId).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    // Nothing changed.
    mockMvc
        .perform(get("/api/brew-methods/{id}", methodId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.name")
                .value(brewMethodRepository.findById(methodId).orElseThrow().getName()));
  }

  @Test
  void admin_canCreateUpdateAndDeleteBrewMethods() throws Exception {
    String email = "method-admin-" + System.nanoTime() + "@example.com";
    registerAndGetToken(email);
    User admin = userRepository.findByEmail(email).orElseThrow();
    admin.setRole(Role.ADMIN);
    userRepository.save(admin);
    String token = loginAndGetToken(email);

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));

    String created =
        mockMvc
            .perform(
                post("/api/brew-methods")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(methodBody("Admin Method")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Number methodId = JsonPath.read(created, "$.id");

    mockMvc
        .perform(
            put("/api/brew-methods/{id}", methodId.longValue())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Admin Method Renamed")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            delete("/api/brew-methods/{id}", methodId.longValue())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void newAccount_isRegularUser() throws Exception {
    String token = registerAndGetToken("method-new-" + System.nanoTime() + "@example.com");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void anonymous_writeIsUnauthorizedNotForbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Anonymous Method")))
        .andExpect(status().isUnauthorized());
  }

  private Long seedMethod() {
    return brewMethodRepository
        .save(BrewMethod.builder().name("Authz Method " + System.nanoTime()).build())
        .getId();
  }

  private static String methodBody(String name) {
    return "{\"name\":\"" + name + " " + System.nanoTime() + "\"}";
  }

  private String registerAndGetToken(String email) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.token");
  }

  private String loginAndGetToken(String email) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.token");
  }
}
