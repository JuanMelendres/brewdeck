package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.auth.emailchange.EmailChange;
import com.brewdeck.brewdeck_api.auth.emailchange.EmailChangeMailPort;
import com.brewdeck.brewdeck_api.auth.emailchange.EmailChangeRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.brewdeck.brewdeck_api.common.security.SecureTokens;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagAdminService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Email change end to end (ADR-019), with the {@code auth-email-change} flag on and off. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailChangeIntegrationTest extends PostgresIntegrationTest {

  private static final String TEST_ENVIRONMENT = "test";
  private static final String PASSWORD = "brew-test-pass-123";

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureFlagAdminService featureFlagAdminService;
  @Autowired private EmailChangeRepository emailChangeRepository;
  @Autowired private UserRepository userRepository;
  @MockitoSpyBean private EmailChangeMailPort mailPort;

  private void setFlag(String key, boolean enabled) {
    featureFlagAdminService.setEnabled(key, TEST_ENVIRONMENT, enabled, "integration-test");
  }

  @BeforeEach
  void turnTheFlagOn() {
    setFlag(FeatureKeys.EMAIL_CHANGE, true);
  }

  @AfterEach
  void turnTheFlagsBackOff() {
    // The test database is shared: never leave a flag on for other test classes.
    setFlag(FeatureKeys.EMAIL_CHANGE, false);
    setFlag(FeatureKeys.REQUIRE_EMAIL_VERIFICATION, false);
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@example.com";
  }

  private static String credentials(String email) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
  }

  /** A logged-in session: the bearer header value and the refresh cookie. */
  private record Session(String bearer, Cookie refresh) {}

  private Session registerAndLogin(String email) throws Exception {
    MockHttpServletResponse login = TestAccounts.registerAndLogin(mockMvc, credentials(email));
    return new Session(
        "Bearer " + JsonPath.read(login.getContentAsString(), "$.token"),
        login.getCookie("brewdeck_refresh"));
  }

  private ResultActions requestChange(Session session, String newEmail, String password)
      throws Exception {
    return mockMvc.perform(
        post("/api/auth/me/email-change")
            .header("Authorization", session.bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"newEmail\":\"" + newEmail + "\",\"currentPassword\":\"" + password + "\"}"));
  }

  private ResultActions postToken(String path, String token) throws Exception {
    return mockMvc.perform(
        post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"));
  }

  private String capturedConfirmToken(String newEmail) {
    ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendConfirmLink(eq(newEmail), token.capture(), any());
    return token.getValue();
  }

  private String capturedCancelToken(String oldEmail) {
    ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
    verify(mailPort).sendChangeRequestedNotice(eq(oldEmail), anyString(), token.capture(), any());
    return token.getValue();
  }

  private ResultActions login(String email) throws Exception {
    return mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentials(email)));
  }

  @Test
  void confirm_movesTheAccountToTheNewAddress_andClosesEverySession() throws Exception {
    String oldEmail = uniqueEmail("change-old");
    String newEmail = uniqueEmail("change-new");
    Session session = registerAndLogin(oldEmail);

    requestChange(session, newEmail, PASSWORD)
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.message").exists());
    String confirmToken = capturedConfirmToken(newEmail);

    postToken("/api/auth/email-change/confirm", confirmToken).andExpect(status().isNoContent());

    // Every session is closed.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", session.bearer()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/auth/refresh").cookie(session.refresh()).header("X-Requested-With", "fetch"))
        .andExpect(status().isUnauthorized());

    // The new address logs in and is verified; the old one no longer exists.
    String token =
        JsonPath.read(
            login(newEmail)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.token");
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(newEmail))
        .andExpect(jsonPath("$.emailVerified").value(true));
    login(oldEmail).andExpect(status().isUnauthorized());
    verify(mailPort).sendEmailChangedNotice(eq(oldEmail), anyString(), any());

    // The link is single use.
    postToken("/api/auth/email-change/confirm", confirmToken).andExpect(status().isBadRequest());
  }

  @Test
  void cancel_stopsTheChange_andClosesEverySession() throws Exception {
    String oldEmail = uniqueEmail("cancel-old");
    String newEmail = uniqueEmail("cancel-new");
    Session session = registerAndLogin(oldEmail);
    requestChange(session, newEmail, PASSWORD).andExpect(status().isAccepted());
    String confirmToken = capturedConfirmToken(newEmail);
    String cancelToken = capturedCancelToken(oldEmail);

    postToken("/api/auth/email-change/cancel", cancelToken).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", session.bearer()))
        .andExpect(status().isUnauthorized());
    postToken("/api/auth/email-change/confirm", confirmToken).andExpect(status().isBadRequest());
    postToken("/api/auth/email-change/cancel", cancelToken).andExpect(status().isBadRequest());
    login(oldEmail).andExpect(status().isOk());
  }

  @Test
  void request_takenAddress_answersTheSame_andStoresNothing() throws Exception {
    String email = uniqueEmail("taken-requester");
    String takenEmail = uniqueEmail("taken-owner");
    registerAndLogin(takenEmail);
    Session session = registerAndLogin(email);

    String freeBody =
        requestChange(session, uniqueEmail("taken-free"), PASSWORD)
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String takenBody =
        requestChange(session, takenEmail, PASSWORD)
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(takenBody).isEqualTo(freeBody);
    verify(mailPort).sendAddressTakenNotice(eq(takenEmail), any());
    Long userId = userRepository.findByEmail(email).orElseThrow().getId();
    assertThat(emailChangeRepository.findByUserIdAndConfirmedAtIsNullAndCancelledAtIsNull(userId))
        .extracting(EmailChange::getNewEmail)
        .doesNotContain(takenEmail);
  }

  @Test
  void request_newerRequest_replacesThePendingOne() throws Exception {
    String email = uniqueEmail("replace-old");
    String first = uniqueEmail("replace-first");
    String second = uniqueEmail("replace-second");
    Session session = registerAndLogin(email);

    requestChange(session, first, PASSWORD).andExpect(status().isAccepted());
    String firstToken = capturedConfirmToken(first);
    requestChange(session, second, PASSWORD).andExpect(status().isAccepted());

    postToken("/api/auth/email-change/confirm", firstToken).andExpect(status().isBadRequest());
    postToken("/api/auth/email-change/confirm", capturedConfirmToken(second))
        .andExpect(status().isNoContent());
  }

  @Test
  void request_wrongPassword_isRejected() throws Exception {
    Session session = registerAndLogin(uniqueEmail("wrong-pass"));

    requestChange(session, uniqueEmail("wrong-pass-new"), "not-my-password-123")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Current password is incorrect"));
  }

  @Test
  void request_sameEmail_isAFieldError() throws Exception {
    String email = uniqueEmail("same");
    Session session = registerAndLogin(email);

    requestChange(session, email.toUpperCase(java.util.Locale.ROOT), PASSWORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.newEmail").value("This is already your email"));
  }

  @Test
  void request_anonymous_isUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/me/email-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"newEmail\":\"x@example.com\",\"currentPassword\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void confirm_expiredLink_isRejected() throws Exception {
    String email = uniqueEmail("expired");
    registerAndLogin(email);
    Long userId = userRepository.findByEmail(email).orElseThrow().getId();
    Instant past = Instant.now().minus(Duration.ofHours(2));
    emailChangeRepository.save(
        EmailChange.builder()
            .userId(userId)
            .newEmail(uniqueEmail("expired-new"))
            .confirmTokenHash(SecureTokens.sha256Hex("expired-confirm-" + userId))
            .cancelTokenHash(SecureTokens.sha256Hex("expired-cancel-" + userId))
            .expiresAt(past.plus(Duration.ofHours(1)))
            .createdAt(past)
            .build());

    postToken("/api/auth/email-change/confirm", "expired-confirm-" + userId)
        .andExpect(status().isBadRequest());
    postToken("/api/auth/email-change/cancel", "expired-cancel-" + userId)
        .andExpect(status().isBadRequest());
  }

  @Test
  void flagOff_blocksRequestAndConfirm_butNotCancel() throws Exception {
    String oldEmail = uniqueEmail("flag-old");
    String newEmail = uniqueEmail("flag-new");
    Session session = registerAndLogin(oldEmail);
    requestChange(session, newEmail, PASSWORD).andExpect(status().isAccepted());
    String confirmToken = capturedConfirmToken(newEmail);
    String cancelToken = capturedCancelToken(oldEmail);

    setFlag(FeatureKeys.EMAIL_CHANGE, false);

    requestChange(session, uniqueEmail("flag-other"), PASSWORD).andExpect(status().isNotFound());
    postToken("/api/auth/email-change/confirm", confirmToken).andExpect(status().isNotFound());
    postToken("/api/auth/email-change/cancel", cancelToken).andExpect(status().isNoContent());
  }

  @Test
  void unverifiedUser_canStillChangeTheEmail_whenVerificationIsEnforced() throws Exception {
    // A mistyped signup address can only be fixed this way (ADR-019).
    Session session = registerAndLogin(uniqueEmail("typo"));
    setFlag(FeatureKeys.REQUIRE_EMAIL_VERIFICATION, true);

    mockMvc
        .perform(get("/api/coffees").header("Authorization", session.bearer()))
        .andExpect(status().isForbidden());
    requestChange(session, uniqueEmail("typo-fixed"), PASSWORD).andExpect(status().isAccepted());
  }

  @Test
  void featureFlags_exposeTheFlagToTheWeb() throws Exception {
    Session session = registerAndLogin(uniqueEmail("flag-alias"));

    mockMvc
        .perform(get("/api/feature-flags").header("Authorization", session.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.emailChange").value(true));
  }
}
