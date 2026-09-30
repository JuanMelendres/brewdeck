package com.brewdeck.brewdeck_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every timestamp in the API is an unambiguous UTC instant ("...Z") that round-trips to the real
 * moment, whatever the JVM or database zone. Clients render it in the viewer's zone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TimestampFormatIntegrationTest extends PostgresIntegrationTest {

  private static final String UTC_INSTANT =
      "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$";

  @Autowired private MockMvc mockMvc;

  @Test
  void resourceUserAndErrorTimestamps_areUtcInstantsCloseToNow() throws Exception {
    Instant before = Instant.now();
    String registered =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"ts-"
                            + System.nanoTime()
                            + "@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String bearer = "Bearer " + JsonPath.read(registered, "$.token");

    String me =
        mockMvc
            .perform(get("/api/auth/me").header("Authorization", bearer))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String coffee =
        mockMvc
            .perform(
                post("/api/coffees")
                    .header("Authorization", bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Timestamp coffee\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String error =
        mockMvc
            .perform(get("/api/coffees/999999999").header("Authorization", bearer))
            .andReturn()
            .getResponse()
            .getContentAsString();

    for (String timestamp :
        new String[] {
          JsonPath.read(me, "$.createdAt"),
          JsonPath.read(coffee, "$.createdAt"),
          JsonPath.read(error, "$.timestamp"),
          JsonPath.read(registered, "$.expiresAt")
        }) {
      assertThat(timestamp).matches(UTC_INSTANT);
    }

    // Stored and read back as the same absolute moment (no zone shift on the way).
    Instant created = Instant.parse(JsonPath.read(coffee, "$.createdAt"));
    assertThat(Duration.between(before, created).abs()).isLessThan(Duration.ofMinutes(1));
  }
}
