package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.Role;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Brew methods are two-tier: a shared catalog (read by all, written only by admins through {@code
 * /api/admin/brew-methods}) plus private methods each user owns. Uses real JWTs so identity and
 * role travel through {@code JwtAuthenticationFilter}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrewMethodOwnershipIntegrationTest extends PostgresIntegrationTest {

  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private CoffeeRepository coffeeRepository;

  @Test
  void privateMethod_isVisibleAndEditableOnlyByItsOwner() throws Exception {
    String owner = registerAndGetToken(uniqueEmail("owner"));
    String stranger = registerAndGetToken(uniqueEmail("stranger"));

    String created =
        mockMvc
            .perform(
                post("/api/brew-methods")
                    .header("Authorization", bearer(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(methodBody("My V60")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shared").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long methodId = ((Number) JsonPath.read(created, "$.id")).longValue();

    // Owner sees it in the list and by id.
    assertThat(listedIds(owner)).contains(methodId);
    mockMvc
        .perform(get("/api/brew-methods/{id}", methodId).header("Authorization", bearer(owner)))
        .andExpect(status().isOk());

    // Another user cannot see, edit, or delete it -- it does not exist for them.
    assertThat(listedIds(stranger)).doesNotContain(methodId);
    mockMvc
        .perform(get("/api/brew-methods/{id}", methodId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/api/brew-methods/{id}", methodId)
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Hijacked")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            delete("/api/brew-methods/{id}", methodId).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());

    // Owner edits and deletes it.
    mockMvc
        .perform(
            put("/api/brew-methods/{id}", methodId)
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("My V60 Renamed")))
        .andExpect(status().isOk());
    mockMvc
        .perform(delete("/api/brew-methods/{id}", methodId).header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());
  }

  @Test
  void sharedMethod_isReadOnlyForRegularUsers() throws Exception {
    String token = registerAndGetToken(uniqueEmail("reader"));
    BrewMethod shared =
        brewMethodRepository.save(
            BrewMethod.builder().name("Shared Method " + System.nanoTime()).build());

    assertThat(listedIds(token)).contains(shared.getId());
    mockMvc
        .perform(
            get("/api/brew-methods/{id}", shared.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shared").value(true));

    mockMvc
        .perform(
            put("/api/brew-methods/{id}", shared.getId())
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Hijacked")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value("Insufficient permissions"));
    mockMvc
        .perform(
            delete("/api/brew-methods/{id}", shared.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isForbidden());

    assertThat(brewMethodRepository.findById(shared.getId()).orElseThrow().getName())
        .isEqualTo(shared.getName());
  }

  @Test
  void regularUser_cannotUseAdminCatalogEndpoints() throws Exception {
    String token = registerAndGetToken(uniqueEmail("not-admin"));

    mockMvc
        .perform(
            post("/api/admin/brew-methods")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Sneaky Shared")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Insufficient permissions"));
  }

  @Test
  void admin_managesSharedCatalog_visibleToEveryone() throws Exception {
    String admin = registerAdminAndGetToken(uniqueEmail("admin"));
    String user = registerAndGetToken(uniqueEmail("catalog-user"));

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", bearer(admin)))
        .andExpect(jsonPath("$.role").value("ADMIN"));

    String created =
        mockMvc
            .perform(
                post("/api/admin/brew-methods")
                    .header("Authorization", bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(methodBody("Catalog Siphon")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shared").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long methodId = ((Number) JsonPath.read(created, "$.id")).longValue();

    assertThat(listedIds(user)).contains(methodId);

    mockMvc
        .perform(
            put("/api/admin/brew-methods/{id}", methodId)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Catalog Siphon Renamed")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            delete("/api/admin/brew-methods/{id}", methodId).header("Authorization", bearer(admin)))
        .andExpect(status().isNoContent());
  }

  @Test
  void admin_cannotTouchAnotherUsersPrivateMethodThroughTheCatalog() throws Exception {
    String admin = registerAdminAndGetToken(uniqueEmail("admin-private"));
    String owner = registerAndGetToken(uniqueEmail("private-owner"));
    String created =
        mockMvc
            .perform(
                post("/api/brew-methods")
                    .header("Authorization", bearer(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(methodBody("Owner Only")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long methodId = ((Number) JsonPath.read(created, "$.id")).longValue();

    mockMvc
        .perform(
            delete("/api/admin/brew-methods/{id}", methodId).header("Authorization", bearer(admin)))
        .andExpect(status().isNotFound());
    assertThat(brewMethodRepository.findById(methodId)).isPresent();
  }

  @Test
  void recipe_cannotUseAnotherUsersPrivateMethod() throws Exception {
    String ownerEmail = uniqueEmail("method-owner");
    String owner = registerAndGetToken(ownerEmail);
    String strangerEmail = uniqueEmail("recipe-stranger");
    String stranger = registerAndGetToken(strangerEmail);

    String created =
        mockMvc
            .perform(
                post("/api/brew-methods")
                    .header("Authorization", bearer(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(methodBody("Secret Method")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long foreignMethodId = ((Number) JsonPath.read(created, "$.id")).longValue();

    User strangerUser = userRepository.findByEmail(strangerEmail).orElseThrow();
    Coffee coffee =
        coffeeRepository.save(Coffee.builder().owner(strangerUser).name("Stranger Coffee").build());

    mockMvc
        .perform(
            post("/api/recipes")
                .header("Authorization", bearer(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"coffeeId\":"
                        + coffee.getId()
                        + ",\"methodId\":"
                        + foreignMethodId
                        + ",\"name\":\"Borrowed\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Brew method not found"));
  }

  @Test
  void twoUsers_mayEachOwnAPrivateMethodWithTheSameName() throws Exception {
    String first = registerAndGetToken(uniqueEmail("same-name-1"));
    String second = registerAndGetToken(uniqueEmail("same-name-2"));
    String body = "{\"name\":\"My Pour Over " + System.nanoTime() + "\"}";

    for (String token : List.of(first, second)) {
      mockMvc
          .perform(
              post("/api/brew-methods")
                  .header("Authorization", bearer(token))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isCreated());
    }

    // ...but one user cannot own the same name twice.
    mockMvc
        .perform(
            post("/api/brew-methods")
                .header("Authorization", bearer(first))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void anonymous_writeIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Anonymous Method")))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/admin/brew-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(methodBody("Anonymous Shared")))
        .andExpect(status().isUnauthorized());
  }

  private List<Long> listedIds(String token) throws Exception {
    String response =
        mockMvc
            .perform(
                get("/api/brew-methods")
                    .header("Authorization", bearer(token))
                    .param("page", "0")
                    .param("size", "100")
                    .param("sort", "id,desc"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Number> ids = JsonPath.read(response, "$.content[*].id");
    return ids.stream().map(Number::longValue).toList();
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@example.com";
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String methodBody(String name) {
    return "{\"name\":\"" + name + " " + System.nanoTime() + "\"}";
  }

  private String registerAdminAndGetToken(String email) throws Exception {
    registerAndGetToken(email);
    User admin = userRepository.findByEmail(email).orElseThrow();
    admin.setRole(Role.ADMIN);
    userRepository.save(admin);
    return loginAndGetToken(email);
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
