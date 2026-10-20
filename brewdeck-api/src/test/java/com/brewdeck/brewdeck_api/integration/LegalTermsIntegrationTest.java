package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.User;
import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.brewdeck.brewdeck_api.legal.LegalTerms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Both states of the {@code legal-terms} flag (LEGAL-01): status endpoint and sign-up consent. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalTermsIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;
  @Autowired private UserRepository userRepository;

  private void setFlag(boolean enabled) {
    featureFlagAdminService.setEnabled(
        FeatureKeys.LEGAL_TERMS, "test", enabled, "integration-test");
  }

  @AfterEach
  void turnTheFlagBackOff() {
    setFlag(false);
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@example.com";
  }

  private ResultActions register(String email, String acceptedTermsVersion) throws Exception {
    String accepted =
        acceptedTermsVersion == null
            ? ""
            : ",\"acceptedTermsVersion\":\"" + acceptedTermsVersion + "\"";
    return mockMvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"email\":\""
                    + email
                    + "\",\"password\":\"brew-test-pass-123\""
                    + accepted
                    + "}"));
  }

  @Test
  void status_isPublic_andFollowsTheFlag() throws Exception {
    mockMvc
        .perform(get("/api/public/legal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.termsVersion").value(LegalTerms.CURRENT_VERSION))
        .andExpect(jsonPath("$.published").value(false));

    setFlag(true);

    mockMvc.perform(get("/api/public/legal")).andExpect(jsonPath("$.published").value(true));
  }

  @Test
  void register_whilePublished_requiresTheCurrentVersion_andRecordsIt() throws Exception {
    setFlag(true);
    String email = uniqueEmail("terms");

    register(email, null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.acceptedTermsVersion").exists());
    register(email, "1999-01-01").andExpect(status().isBadRequest());
    assertThat(userRepository.findByEmail(email)).isEmpty();

    register(email, LegalTerms.CURRENT_VERSION).andExpect(status().isAccepted());

    User user = userRepository.findByEmail(email).orElseThrow();
    assertThat(user.getTermsVersion()).isEqualTo(LegalTerms.CURRENT_VERSION);
    assertThat(user.getTermsAcceptedAt()).isNotNull();
  }

  @Test
  void register_whileNotPublished_needsNoAcceptance() throws Exception {
    String email = uniqueEmail("no-terms");

    register(email, null).andExpect(status().isAccepted());

    assertThat(userRepository.findByEmail(email).orElseThrow().getTermsVersion()).isNull();
  }
}
