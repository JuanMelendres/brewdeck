package com.brewdeck.brewdeck_api.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/** The real messages*.properties bundles, configured like application.yaml, for unit tests. */
public final class TestMessages {

  private TestMessages() {}

  public static MessageSource messageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    return source;
  }
}
