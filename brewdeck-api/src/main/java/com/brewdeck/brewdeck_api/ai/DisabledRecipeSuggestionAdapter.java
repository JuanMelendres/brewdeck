package com.brewdeck.brewdeck_api.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.ai", name = "enabled", havingValue = "false")
public class DisabledRecipeSuggestionAdapter implements RecipeSuggestionPort {

  @Override
  public SuggestedRecipe suggest(SuggestionContext context) {
    throw new AiUnavailableException("AI suggestions are disabled");
  }
}
