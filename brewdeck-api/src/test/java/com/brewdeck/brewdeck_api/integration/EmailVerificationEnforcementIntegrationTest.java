package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.Role;
import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationMailPort;
import com.brewdeck.brewdeck_api.coffee.CoffeeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Both states of the {@code auth-require-email-verification} flag (ADR-012): off, unverified users
 * keep full access; on, they are limited to the verification endpoints until they verify.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailVerificationEnforcementIntegrationTest extends PostgresIntegrationTest {

  private static final String TEST_ENVIRONMENT = "test";

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;
  @Autowired private UserRepository userRepository;
  @Autowired private CoffeeRepository coffeeRepository;
  @MockitoSpyBean private EmailVerificationMailPort mailPort;

  private void setFlag(boolean enabled) {
    featureFlagAdminService.setEnabled(
        FeatureKeys.REQUIRE_EMAIL_VERIFICATION, TEST_ENVIRONMENT, enabled, "integration-test");
  }

  @AfterEach
  void turnTheFlagBackOff() {
    // The test database is shared: never leave enforcement on for other test classes.
    setFlag(false);
  }

  private String registerUnverified(String email) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(response, "$.token");
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@example.com";
  }

  @Test
  void flagOff_unverifiedUserKeepsFullAccess() throws Exception {
    String bearer = registerUnverified(uniqueEmail("flag-off"));

    mockMvc.perform(get("/api/coffees").header("Authorization", bearer)).andExpect(status().isOk());
  }

  @Test
  void flagOn_unverifiedUserIsBlockedWithACode_andNothingIsWritten() throws Exception {
    String email = uniqueEmail("flag-on");
    String bearer = registerUnverified(email);
    setFlag(true);
    Long userId = userRepository.findByEmail(email).orElseThrow().getId();

    mockMvc
        .perform(get("/api/coffees").header("Authorization", bearer))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
        .andExpect(jsonPath("$.message").value("Verify your email address to continue"));
    mockMvc
        .perform(
            post("/api/coffees")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Blocked coffee\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

    assertThat(coffeeRepository.countByOwnerId(userId)).isZero();
  }

  @Test
  void flagOn_unverifiedUserCanStillFinishVerifying() throws Exception {
    String email = uniqueEmail("flag-on-allowed");
    String bearer = registerUnverified(email);
    setFlag(true);

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(false));
    mockMvc
        .perform(get("/api/feature-flags").header("Authorization", bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.requireEmailVerification").value(true));
    mockMvc
        .perform(post("/api/auth/resend-verification").header("Authorization", bearer))
        .andExpect(status().isOk());

    // Use the link from the resent email: once verified, the very next request goes through.
    ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(mailPort, org.mockito.Mockito.atLeastOnce())
        .sendVerificationLink(org.mockito.ArgumentMatchers.eq(email), token.capture(), any());
    mockMvc
        .perform(
            post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getValue() + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/coffees").header("Authorization", bearer)).andExpect(status().isOk());
  }

  @Test
  void flagOn_verifiedUserIsUnaffected() throws Exception {
    String email = uniqueEmail("flag-on-verified");
    String bearer = registerUnverified(email);
    User user = userRepository.findByEmail(email).orElseThrow();
    user.setEmailVerified(true);
    userRepository.save(user);
    setFlag(true);

    mockMvc.perform(get("/api/coffees").header("Authorization", bearer)).andExpect(status().isOk());
  }

  @Test
  void flagOn_evenAnUnverifiedAdminIsBlocked() throws Exception {
    String email = uniqueEmail("flag-on-admin");
    String bearer = registerUnverified(email);
    User admin = userRepository.findByEmail(email).orElseThrow();
    admin.setRole(Role.ADMIN);
    userRepository.save(admin);
    setFlag(true);

    mockMvc
        .perform(
            post("/api/admin/brew-methods")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Blocked " + System.nanoTime() + "\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
  }

  @Test
  void flagOn_publicEndpointsStayOpen() throws Exception {
    setFlag(true);

    mockMvc
        .perform(get("/api/public/recipes/{token}", "no-such-token"))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/coffees")).andExpect(status().isUnauthorized());
  }
}
