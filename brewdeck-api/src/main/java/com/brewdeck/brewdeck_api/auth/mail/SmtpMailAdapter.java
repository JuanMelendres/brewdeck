package com.brewdeck.brewdeck_api.auth.mail;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetMailPort;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationMailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

/**
 * Real SMTP delivery for verification and password-reset links, active when {@code
 * brewdeck.mail.enabled=true} (the logging adapters are used otherwise). Connection settings come
 * from {@code spring.mail.*} / {@code SPRING_MAIL_*}, so any SMTP provider works.
 *
 * <p>Sending is deferred until the surrounding transaction <b>commits</b> (so a rolled-back request
 * never emails a token that was not saved) and runs on the application task executor (so a slow
 * SMTP server never holds the request or its DB connection). Failures are logged, not thrown: the
 * caller's response no longer depends on mail delivery.
 *
 * <p>Neither the recipient address nor the token is logged. Text comes from {@code email.*} in
 * messages*.properties, in the user's language (ADR-015).
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.mail", name = "enabled", havingValue = "true")
public class SmtpMailAdapter implements EmailVerificationMailPort, PasswordResetMailPort {

  private final JavaMailSender mailSender;
  private final MailProperties properties;
  private final TaskExecutor taskExecutor;
  private final MessageSource messageSource;

  public SmtpMailAdapter(
      JavaMailSender mailSender,
      MailProperties properties,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      MessageSource messageSource) {
    this.mailSender = mailSender;
    this.properties = properties;
    this.taskExecutor = taskExecutor;
    this.messageSource = messageSource;
  }

  @Override
  public void sendVerificationLink(String email, String rawToken, Locale locale) {
    send("verification", email, link("/verify-email", rawToken), locale);
  }

  @Override
  public void sendResetLink(String email, String rawToken, Locale locale) {
    send("password-reset", email, link("/reset-password", rawToken), locale);
  }

  /**
   * Builds the email from the {@code email.<kind>.*} messages in {@code locale}'s language. The
   * locale is resolved here, on the request thread; sending happens later on another thread.
   */
  private void send(String kind, String to, String link, Locale locale) {
    String prefix = "email." + kind + ".";
    String footer = message(prefix + "footer", locale);
    String text = message(prefix + "textIntro", locale) + "\n\n" + link + "\n\n" + footer;
    dispatch(
        kind,
        to,
        message(prefix + "subject", locale),
        text,
        html(
            locale,
            message(prefix + "heading", locale),
            message(prefix + "intro", locale),
            message(prefix + "button", locale),
            link,
            message("email.pasteLink", locale),
            footer));
  }

  private String message(String key, Locale locale) {
    return messageSource.getMessage(key, null, locale);
  }

  private void dispatch(String kind, String to, String subject, String text, String html) {
    Runnable send = () -> sendNow(kind, to, subject, text, html);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              taskExecutor.execute(send);
            }
          });
    } else {
      taskExecutor.execute(send);
    }
  }

  void sendNow(String kind, String to, String subject, String text, String html) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(properties.from());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, html);
      mailSender.send(message);
      log.info("Sent {} email", kind);
    } catch (MailException | MessagingException e) {
      log.error("Failed to send {} email", kind, e);
    }
  }

  private String link(String path, String rawToken) {
    return properties.frontendBaseUrl()
        + path
        + "?token="
        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
  }

  private static String html(
      Locale locale,
      String heading,
      String intro,
      String buttonLabel,
      String link,
      String pasteLink,
      String footer) {
    String safeLink = HtmlUtils.htmlEscape(link);
    return """
        <div lang="%s" style="font-family:system-ui,sans-serif;max-width:480px;margin:auto">
          <h2>%s</h2>
          <p>%s</p>
          <p><a href="%s" style="display:inline-block;padding:10px 16px;background:#6f4e37;\
        color:#fff;text-decoration:none;border-radius:6px">%s</a></p>
          <p style="font-size:12px;color:#666">%s<br>%s</p>
          <p style="font-size:12px;color:#666">%s</p>
        </div>
        """
        .formatted(
            locale.toLanguageTag(),
            escape(heading),
            escape(intro),
            safeLink,
            escape(buttonLabel),
            escape(pasteLink),
            safeLink,
            escape(footer));
  }

  /** Escapes markup but keeps accents readable in the UTF-8 email. */
  private static String escape(String text) {
    return HtmlUtils.htmlEscape(text, StandardCharsets.UTF_8.name());
  }
}
