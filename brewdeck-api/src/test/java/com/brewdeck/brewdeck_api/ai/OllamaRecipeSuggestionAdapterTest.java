package com.brewdeck.brewdeck_api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaRecipeSuggestionAdapterTest {

  private static final String ANSWER =
      "{\\\"coffeeGrams\\\":15,\\\"waterGrams\\\":250,\\\"ratio\\\":\\\"1:16.7\\\","
          + "\\\"grindSetting\\\":\\\"medium-fine\\\",\\\"waterTemp\\\":93,\\\"brewTime\\\":\\\"2:30\\\","
          + "\\\"steps\\\":\\\"Bloom, then pour.\\\",\\\"rationale\\\":\\\"Balanced.\\\"}";

  private MockRestServiceServer server;
  private OllamaRecipeSuggestionAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    AiProperties properties =
        new AiProperties(
            true,
            "ollama",
            "claude-haiku-4-5",
            20,
            1024,
            new AiProperties.Ollama("http://ollama.test", "qwen3:8b", 90, 1024));
    adapter = new OllamaRecipeSuggestionAdapter(properties, builder, new ObjectMapper());
  }

  @AfterEach
  void clearLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  private static SuggestionContext context() {
    return new SuggestionContext(
        "Finca La Pastoria", "Chiapas", "Light", "Washed", 4, 2, 4, 1, "V60", "Pour-over", null);
  }

  private static String ollamaReply(String content, String doneReason) {
    return "{\"model\":\"qwen3:8b\",\"message\":{\"role\":\"assistant\",\"content\":\""
        + content
        + "\"},\"done\":true,\"done_reason\":\""
        + doneReason
        + "\"}";
  }

  @Test
  void suggest_sendsTheSchemaAndLimits_andParsesTheAnswer() {
    server
        .expect(requestTo("http://ollama.test/api/chat"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    "{\"model\":\"qwen3:8b\",\"stream\":false,\"think\":false,"
                        + "\"options\":{\"num_predict\":1024},"
                        + "\"format\":{\"type\":\"object\"}}"))
        .andRespond(withSuccess(ollamaReply(ANSWER, "stop"), MediaType.APPLICATION_JSON));

    SuggestedRecipe recipe = adapter.suggest(context());

    assertThat(recipe.coffeeGrams()).isEqualByComparingTo(BigDecimal.valueOf(15));
    assertThat(recipe.waterGrams()).isEqualByComparingTo(BigDecimal.valueOf(250));
    assertThat(recipe.waterTemp()).isEqualTo(93);
    assertThat(recipe.steps()).isEqualTo("Bloom, then pour.");
    server.verify();
  }

  @Test
  void suggest_asksForSpanishWhenTheRequestIsInSpanish() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
    server
        .expect(requestTo("http://ollama.test/api/chat"))
        .andExpect(
            request -> {
              String body = request.getBody().toString();
              assertThat(body).contains("Write steps and rationale in Spanish.");
            })
        .andRespond(withSuccess(ollamaReply(ANSWER, "stop"), MediaType.APPLICATION_JSON));

    adapter.suggest(context());

    server.verify();
  }

  @Test
  void aTruncatedAnswer_isAFailure() {
    server
        .expect(requestTo("http://ollama.test/api/chat"))
        .andRespond(
            withSuccess(
                ollamaReply("{\\\"coffeeGrams\\\":15,", "length"), MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> adapter.suggest(context())).isInstanceOf(AiUnavailableException.class);
  }

  @Test
  void anAnswerThatIsNotJson_isAFailure() {
    server
        .expect(requestTo("http://ollama.test/api/chat"))
        .andRespond(
            withSuccess(ollamaReply("Sure! Here is a recipe", "stop"), MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> adapter.suggest(context())).isInstanceOf(AiUnavailableException.class);
  }

  @Test
  void anOllamaError_isAFailure() {
    server.expect(requestTo("http://ollama.test/api/chat")).andRespond(withServerError());

    assertThatThrownBy(
            () ->
                adapter.improve(
                    new ImprovementContext(
                        "Coffee", null, null, null, null, null, null, null, "V60", null, null, null,
                        null, null, null, null, null, List.of())))
        .isInstanceOf(AiUnavailableException.class);
  }
}
