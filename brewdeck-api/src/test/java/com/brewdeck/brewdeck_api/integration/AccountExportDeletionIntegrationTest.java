package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.account.AccountMailPort;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.emailchange.EmailChangeRepository;
import com.brewdeck.brewdeck_api.auth.refresh.RefreshTokenRepository;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetTokenRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationTokenRepository;
import com.brewdeck.brewdeck_api.coffee.Coffee;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.brewdeck.brewdeck_api.method.BrewMethod;
import com.brewdeck.brewdeck_api.method.BrewMethodRepository;
import com.brewdeck.brewdeck_api.recipe.Recipe;
import com.brewdeck.brewdeck_api.recipe.RecipeRepository;
import com.brewdeck.brewdeck_api.session.BrewSession;
import com.brewdeck.brewdeck_api.session.BrewSessionRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Data export and account deletion end to end (ADR-020). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountExportDeletionIntegrationTest extends PostgresIntegrationTest {

  private static final String PASSWORD = "brew-test-pass-123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private CoffeeRepository coffeeRepository;
  @Autowired private BrewMethodRepository brewMethodRepository;
  @Autowired private RecipeRepository recipeRepository;
  @Autowired private BrewSessionRepository brewSessionRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Autowired private EmailChangeRepository emailChangeRepository;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;
  @MockitoSpyBean private AccountMailPort mailPort;

  @AfterEach
  void turnFlagsBackOff() {
    featureFlagAdminService.setEnabled(FeatureKeys.EMAIL_CHANGE, "test", false, "integration-test");
    featureFlagAdminService.setEnabled(
        FeatureKeys.REQUIRE_EMAIL_VERIFICATION, "test", false, "integration-test");
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@example.com";
  }

  private static String credentials(String email) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
  }

  private record Session(String bearer, Cookie refresh) {}

  private Session registerAndLogin(String email) throws Exception {
    MockHttpServletResponse login = TestAccounts.registerAndLogin(mockMvc, credentials(email));
    return new Session(
        "Bearer " + JsonPath.read(login.getContentAsString(), "$.token"),
        login.getCookie("brewdeck_refresh"));
  }

  /** One of everything a user can own: coffee, own method, recipe and brew session. */
  private void seedOwnedData(String email, String label) {
    User owner = userRepository.findByEmail(email).orElseThrow();
    Coffee coffee =
        coffeeRepository.save(Coffee.builder().name(label + " Coffee").owner(owner).build());
    BrewMethod method =
        brewMethodRepository.save(
            BrewMethod.builder().name(label + " Method " + System.nanoTime()).owner(owner).build());
    Recipe recipe =
        recipeRepository.save(
            Recipe.builder()
                .coffee(coffee)
                .method(method)
                .name(label + " Recipe")
                .favorite(false)
                .owner(owner)
                .build());
    brewSessionRepository.save(
        BrewSession.builder()
            .recipe(recipe)
            .brewedAt(Instant.now())
            .rating(8)
            .owner(owner)
            .build());
  }

  private ResultActions deleteAccount(Session session, String password) throws Exception {
    return mockMvc.perform(
        delete("/api/auth/me")
            .header("Authorization", session.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\":\"" + password + "\"}"));
  }

  @Test
  void export_returnsOnlyTheCallersData_asAnAttachment_withoutSecrets() throws Exception {
    String email = uniqueEmail("export");
    Session session = registerAndLogin(email);
    seedOwnedData(email, "Mine");
    String other = uniqueEmail("export-other");
    registerAndLogin(other);
    seedOwnedData(other, "Theirs");

    String body =
        mockMvc
            .perform(get("/api/auth/me/export").header("Authorization", session.bearer()))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.startsWith(
                            "attachment; filename=\"brewdeck-export-")))
            .andExpect(jsonPath("$.profile.email").value(email))
            .andExpect(jsonPath("$.coffees.length()").value(1))
            .andExpect(jsonPath("$.coffees[0].name").value("Mine Coffee"))
            .andExpect(jsonPath("$.brewMethods.length()").value(1))
            .andExpect(jsonPath("$.recipes[0].name").value("Mine Recipe"))
            .andExpect(jsonPath("$.brewSessions[0].rating").value(8))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body)
        .doesNotContain("Theirs")
        .doesNotContain("argon2")
        .doesNotContain("passwordHash");
  }

  @Test
  void delete_wrongPassword_keepsEverything() throws Exception {
    String email = uniqueEmail("delete-wrong");
    Session session = registerAndLogin(email);

    deleteAccount(session, "not-my-password-123")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Current password is incorrect"));

    assertThat(userRepository.findByEmail(email)).isPresent();
    verify(mailPort, never()).sendAccountDeletedNotice(eq(email), any());
  }

  @Test
  void delete_removesEveryOwnedRow_endsTheSessions_andFreesTheAddress() throws Exception {
    String email = uniqueEmail("delete");
    Session session = registerAndLogin(email);
    seedOwnedData(email, "Doomed");
    String other = uniqueEmail("delete-bystander");
    registerAndLogin(other);
    seedOwnedData(other, "Kept");
    User user = userRepository.findByEmail(email).orElseThrow();
    Long id = user.getId();
    // Token rows of every kind: refresh (login), verification (sign-up), reset, email change.
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk());
    featureFlagAdminService.setEnabled(FeatureKeys.EMAIL_CHANGE, "test", true, "integration-test");
    mockMvc
        .perform(
            post("/api/auth/me/email-change")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"newEmail\":\""
                        + uniqueEmail("delete-new")
                        + "\",\"currentPassword\":\""
                        + PASSWORD
                        + "\"}"))
        .andExpect(status().isAccepted());
    long catalogMethodsBefore = brewMethodRepository.countVisibleTo(null);

    deleteAccount(session, PASSWORD)
        .andExpect(status().isNoContent())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

    assertThat(userRepository.findById(id)).isEmpty();
    assertThat(coffeeRepository.countByOwnerId(id)).isZero();
    assertThat(recipeRepository.countByOwnerId(id)).isZero();
    assertThat(brewSessionRepository.countByOwnerId(id)).isZero();
    assertThat(brewMethodRepository.findAllByOwnerIdOrderByIdAsc(id)).isEmpty();
    assertThat(refreshTokenRepository.findAll()).noneMatch(t -> id.equals(t.getUserId()));
    assertThat(passwordResetTokenRepository.findAll()).noneMatch(t -> id.equals(t.getUserId()));
    assertThat(emailVerificationTokenRepository.findAll()).noneMatch(t -> id.equals(t.getUserId()));
    assertThat(emailChangeRepository.findAll()).noneMatch(c -> id.equals(c.getUserId()));
    assertThat(brewMethodRepository.countVisibleTo(null)).isEqualTo(catalogMethodsBefore);
    Long otherId = userRepository.findByEmail(other).orElseThrow().getId();
    assertThat(coffeeRepository.countByOwnerId(otherId)).isEqualTo(1);
    verify(mailPort).sendAccountDeletedNotice(eq(email), any());

    // Every session is gone.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", session.bearer()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/auth/refresh").cookie(session.refresh()).header("X-Requested-With", "fetch"))
        .andExpect(status().isUnauthorized());

    // The address is free: signing up again gives a new, empty account.
    Session again = registerAndLogin(email);
    mockMvc
        .perform(get("/api/auth/me/export").header("Authorization", again.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coffees.length()").value(0));
  }

  @Test
  void unverifiedUsers_canExportAndDelete_whileVerificationIsEnforced() throws Exception {
    String email = uniqueEmail("unverified");
    Session session = registerAndLogin(email);
    featureFlagAdminService.setEnabled(
        FeatureKeys.REQUIRE_EMAIL_VERIFICATION, "test", true, "integration-test");

    mockMvc
        .perform(get("/api/auth/me/export").header("Authorization", session.bearer()))
        .andExpect(status().isOk());
    deleteAccount(session, PASSWORD).andExpect(status().isNoContent());
  }

  @Test
  void anonymousCalls_areUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/me/export")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            delete("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isUnauthorized());
  }
}
