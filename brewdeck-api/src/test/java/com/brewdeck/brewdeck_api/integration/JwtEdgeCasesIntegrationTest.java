package com.brewdeck.brewdeck_api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.auth.UserRepository;
import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Any access token that is not a current, correctly signed token for an existing user must be
 * treated as anonymous: protected endpoints answer 401, never 200 or 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtEdgeCasesIntegrationTest extends PostgresIntegrationTest {

  private static final String PROTECTED = "/api/auth/me";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbc;

  @Value("${brewdeck.auth.secret}")
  private String secret;

  private SecretKey appKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  private ResultActions callWith(String authorizationHeader) throws Exception {
    return mockMvc.perform(get(PROTECTED).header("Authorization", authorizationHeader));
  }

  private void expectUnauthorized(String authorizationHeader) throws Exception {
    callWith(authorizationHeader)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Authentication required"));
  }

  private String registerAndGetToken(String email) throws Exception {
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
    return JsonPath.read(response, "$.token");
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@example.com";
  }

  @Test
  void validToken_isAccepted_asABaseline() throws Exception {
    String token = registerAndGetToken(uniqueEmail("jwt-ok"));

    callWith("Bearer " + token).andExpect(status().isOk());
  }

  @Test
  void expiredToken_isRejected() throws Exception {
    String email = uniqueEmail("jwt-expired");
    registerAndGetToken(email);
    Instant past = Instant.now().minusSeconds(3600);
    String expired =
        Jwts.builder()
            .subject(email)
            .issuedAt(Date.from(past.minusSeconds(900)))
            .expiration(Date.from(past))
            .signWith(appKey())
            .compact();

    expectUnauthorized("Bearer " + expired);
  }

  @Test
  void tamperedPayload_isRejected() throws Exception {
    String victim = uniqueEmail("jwt-victim");
    registerAndGetToken(victim);
    String attackerToken = registerAndGetToken(uniqueEmail("jwt-attacker"));

    // Swap in a payload naming the victim but keep the attacker's signature.
    String[] parts = attackerToken.split("\\.");
    String forgedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"sub\":\""
                        + victim
                        + "\",\"exp\":"
                        + (Instant.now().getEpochSecond() + 600)
                        + "}")
                    .getBytes(StandardCharsets.UTF_8));

    expectUnauthorized("Bearer " + parts[0] + "." + forgedPayload + "." + parts[2]);
  }

  @Test
  void tokenSignedWithAnotherKey_isRejected() throws Exception {
    String email = uniqueEmail("jwt-other-key");
    registerAndGetToken(email);
    SecretKey attackerKey =
        Keys.hmacShaKeyFor(
            "an-attacker-controlled-secret-that-is-long-enough-for-hs384!"
                .getBytes(StandardCharsets.UTF_8));
    String forged =
        Jwts.builder()
            .subject(email)
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(attackerKey)
            .compact();

    expectUnauthorized("Bearer " + forged);
  }

  @Test
  void unsignedAlgNoneToken_isRejected() throws Exception {
    String email = uniqueEmail("jwt-none");
    registerAndGetToken(email);
    String unsigned =
        Jwts.builder()
            .subject(email)
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .compact();

    expectUnauthorized("Bearer " + unsigned);
  }

  @Test
  void validTokenForADeletedUser_isRejected() throws Exception {
    String email = uniqueEmail("jwt-deleted");
    String token = registerAndGetToken(email);
    // Remove the account out from under a still-valid token (dependent rows first).
    Long userId = userRepository.findByEmail(email).orElseThrow().getId();
    jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM email_verification_tokens WHERE user_id = ?", userId);
    userRepository.deleteById(userId);

    expectUnauthorized("Bearer " + token);
  }

  @Test
  void malformedOrWrongSchemeHeaders_areRejected() throws Exception {
    String token = registerAndGetToken(uniqueEmail("jwt-scheme"));

    expectUnauthorized("Bearer not-a-jwt");
    expectUnauthorized("Bearer ");
    expectUnauthorized("Token " + token);
    expectUnauthorized(token);
  }
}
