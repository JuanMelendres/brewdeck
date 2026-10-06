package com.brewdeck.brewdeck_api.common.i18n;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Resolves the response language from {@code Accept-Language}, limited to the languages BrewDeck
 * ships (ADR-015). Anything else, or no header, gets English. The bean name matters: the
 * DispatcherServlet looks up "localeResolver".
 */
@Configuration
public class LocaleConfig {

  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  public static final List<Locale> SUPPORTED_LOCALES =
      List.of(Locale.ENGLISH, Locale.forLanguageTag("es"));

  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(SUPPORTED_LOCALES);
    resolver.setDefaultLocale(DEFAULT_LOCALE);
    return resolver;
  }
}
