package com.brewdeck.brewdeck_api.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brewdeck.brewdeck_api.common.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spring MVC's own client errors must keep their real status and the standard ErrorResponse body
 * instead of collapsing into a 500 (audit finding 11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class MvcErrorHandlingIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void unsupportedMethod_returns405WithAllowHeader() throws Exception {
    mockMvc
        .perform(delete("/api/coffees"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", containsString("GET")))
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.error").value("Method Not Allowed"))
        .andExpect(jsonPath("$.path").value("/api/coffees"));
  }

  @Test
  void unknownPath_returns404() throws Exception {
    mockMvc
        .perform(get("/api/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void unsupportedContentType_returns415() throws Exception {
    mockMvc
        .perform(post("/api/coffees").contentType(MediaType.TEXT_PLAIN).content("not json"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
  }

  @Test
  void unacceptableResponseType_returns406() throws Exception {
    mockMvc
        .perform(get("/api/coffees").accept(MediaType.APPLICATION_XML))
        .andExpect(status().isNotAcceptable());
  }

  @Test
  void existingSpecificHandlers_areUnchanged() throws Exception {
    mockMvc
        .perform(get("/api/coffees/most-used").param("limit", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid value for parameter 'limit'"));
  }
}
