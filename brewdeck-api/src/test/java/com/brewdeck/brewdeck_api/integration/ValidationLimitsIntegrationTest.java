package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every value the request validation accepts must fit in the database, and anything beyond the
 * limits must be a 400 on the field -- never a 409 from the database or a 500 from BCrypt (audit
 * findings 5 and 12).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationLimitsIntegrationTest extends PostgresIntegrationTest {

  private static final String PASSWORD_TOO_LONG =
      "Password must not exceed 72 bytes (fewer characters if it uses accents or emoji)";

  @Autowired private MockMvc mockMvc;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private RecipeRepository recipeRepository;

  // --- Column widths (V17) ---

  @Test
  @WithMockUser
  void coffee_roastLevelAtItsValidatedMaximum_isStored() throws Exception {
    postJson("/api/coffees", "{\"name\":\"Long roast\",\"roastLevel\":\"" + "r".repeat(80) + "\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.roastLevel").value("r".repeat(80)));
  }

  @Test
  @WithMockUser
  void coffee_roastLevelOverTheLimit_isAFieldError() throws Exception {
    postJson("/api/coffees", "{\"name\":\"Too long\",\"roastLevel\":\"" + "r".repeat(81) + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.roastLevel")
                .value("Roast level must not exceed 80 characters"));
  }

  @Test
  @WithMockUser
  void recipe_grindSettingAtItsValidatedMaximum_isStored() throws Exception {
    postJson(
            "/api/recipes",
            recipeBody("\"grindSetting\":\"" + "g".repeat(120) + "\",\"coffeeGrams\":15"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.grindSetting").value("g".repeat(120)));
  }

  @Test
  @WithMockUser
  void recipe_gramsAboveTheColumnPrecision_isAFieldError() throws Exception {
    postJson("/api/recipes", recipeBody("\"coffeeGrams\":10000,\"waterGrams\":9999.999"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.coffeeGrams")
                .value("Coffee grams must not exceed 9999.99"))
        .andExpect(
            jsonPath("$.validationErrors.waterGrams").value("Water grams must not exceed 9999.99"));
  }

  @Test
  @WithMockUser
  void recipe_gramsAtTheColumnMaximum_isStored() throws Exception {
    postJson("/api/recipes", recipeBody("\"coffeeGrams\":9999.99,\"waterGrams\":9999.99"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.coffeeGrams").value(9999.99));
  }

  @Test
  @WithMockUser
  void brewSession_actualGrindAtItsValidatedMaximum_isStored() throws Exception {
    Long recipeId = seedRecipe();

    postJson(
            "/api/brew-sessions",
            "{\"recipeId\":" + recipeId + ",\"actualGrind\":\"" + "a".repeat(120) + "\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.actualGrind").value("a".repeat(120)));
  }

  // --- Password byte limit (BCrypt) ---

  @Test
  void register_passwordAt72Bytes_succeeds() throws Exception {
    postJson("/api/auth/register", registerBody("x".repeat(72))).andExpect(status().isCreated());
  }

  @Test
  void register_passwordOver72Bytes_isAFieldErrorNotA500() throws Exception {
    postJson("/api/auth/register", registerBody("x".repeat(80)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.password").value(PASSWORD_TOO_LONG));
  }

  @Test
  void register_multiBytePasswordOver72Bytes_isRejectedEvenUnder72Characters() throws Exception {
    // 37 characters, 74 bytes.
    postJson("/api/auth/register", registerBody("é".repeat(37)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.password").value(PASSWORD_TOO_LONG));
  }

  @Test
  void login_passwordOver72Bytes_isABadRequestNotA500() throws Exception {
    postJson(
            "/api/auth/login",
            "{\"email\":\"anyone@example.com\",\"password\":\"" + "x".repeat(80) + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.password").value(PASSWORD_TOO_LONG));
  }

  @Test
  void changePassword_newPasswordOver72Bytes_isAFieldError() throws Exception {
    String registered =
        postJson("/api/auth/register", registerBody("password123"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(registered, "$.token");

    mockMvc
        .perform(
            post("/api/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"password123\",\"newPassword\":\""
                        + "x".repeat(73)
                        + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.newPassword").value(PASSWORD_TOO_LONG));
  }

  private org.springframework.test.web.servlet.ResultActions postJson(String url, String body)
      throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static String registerBody(String password) {
    return "{\"email\":\"limits-"
        + System.nanoTime()
        + "@example.com\",\"password\":\""
        + password
        + "\"}";
  }

  private String recipeBody(String extraFields) {
    Coffee coffee = seedCoffee();
    BrewMethod method = seedSharedMethod();
    return "{\"coffeeId\":"
        + coffee.getId()
        + ",\"methodId\":"
        + method.getId()
        + ",\"name\":\"Limits recipe\","
        + extraFields
        + "}";
  }

  private Coffee seedCoffee() {
    return coffeeRepository.save(Coffee.builder().owner(mockUser()).name("Limits coffee").build());
  }

  private BrewMethod seedSharedMethod() {
    return brewMethodRepository.save(
        BrewMethod.builder().name("Limits method " + System.nanoTime()).build());
  }

  private Long seedRecipe() {
    return recipeRepository
        .save(
            Recipe.builder()
                .owner(mockUser())
                .coffee(seedCoffee())
                .method(seedSharedMethod())
                .name("Limits session recipe")
                .build())
        .getId();
  }
}
