package com.brewdeck.brewdeck_api.auth.mail;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import com.brewdeck.brewdeck_api.auth.reset.PasswordResetMailPort;
import com.brewdeck.brewdeck_api.auth.verification.EmailVerificationMailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>Neither the recipient address nor the token is logged.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "brewdeck.mail", name = "enabled", havingValue = "true")
public class SmtpMailAdapter implements EmailVerificationMailPort, PasswordResetMailPort {

  private final JavaMailSender mailSender;
  private final MailProperties properties;
  private final TaskExecutor taskExecutor;

  public SmtpMailAdapter(
      JavaMailSender mailSender,
      MailProperties properties,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.mailSender = mailSender;
    this.properties = properties;
    this.taskExecutor = taskExecutor;
  }

  @Override
  public void sendVerificationLink(String email, String rawToken) {
    String link = link("/verify-email", rawToken);
    dispatch(
        "verification",
        email,
        "Verify your BrewDeck email",
        "Welcome to BrewDeck!\n\nConfirm your email address by opening this link (valid for 24"
            + " hours):\n\n"
            + link
            + "\n\nIf you did not create a BrewDeck account, ignore this email.",
        html(
            "Welcome to BrewDeck!",
            "Confirm your email address. The link is valid for 24 hours.",
            "Verify email",
            link,
            "If you did not create a BrewDeck account, ignore this email."));
  }

  @Override
  public void sendResetLink(String email, String rawToken) {
    String link = link("/reset-password", rawToken);
    dispatch(
        "password-reset",
        email,
        "Reset your BrewDeck password",
        "Someone asked to reset your BrewDeck password.\n\nChoose a new one here (valid for 30"
            + " minutes):\n\n"
            + link
            + "\n\nIf it wasn't you, ignore this email. Your password stays the same.",
        html(
            "Reset your password",
            "Someone asked to reset your BrewDeck password. The link is valid for 30 minutes.",
            "Choose a new password",
            link,
            "If it wasn't you, ignore this email. Your password stays the same."));
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
      String heading, String intro, String buttonLabel, String link, String footer) {
    String safeLink = HtmlUtils.htmlEscape(link);
    return """
        <div style="font-family:system-ui,sans-serif;max-width:480px;margin:auto">
          <h2>%s</h2>
          <p>%s</p>
          <p><a href="%s" style="display:inline-block;padding:10px 16px;background:#6f4e37;\
        color:#fff;text-decoration:none;border-radius:6px">%s</a></p>
          <p style="font-size:12px;color:#666">Or paste this link into your browser:<br>%s</p>
          <p style="font-size:12px;color:#666">%s</p>
        </div>
        """
        .formatted(heading, intro, safeLink, buttonLabel, safeLink, footer);
  }
}
