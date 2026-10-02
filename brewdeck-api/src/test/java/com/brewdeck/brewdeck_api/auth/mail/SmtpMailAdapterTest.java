package com.brewdeck.brewdeck_api.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SmtpMailAdapterTest {

  private final JavaMailSender mailSender = mock(JavaMailSender.class);
  private final SmtpMailAdapter adapter =
      new SmtpMailAdapter(
          mailSender,
          new MailProperties(
              true, "https://brewdeck.example", "BrewDeck <no-reply@brewdeck.example>"),
          new SyncTaskExecutor());

  @BeforeEach
  void realMimeMessages() {
    when(mailSender.createMimeMessage())
        .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
  }

  @AfterEach
  void clearTransactionSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private MimeMessage sentMessage() {
    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    return captor.getValue();
  }

  /** The text/plain body, wherever MimeMessageHelper nested it. */
  private static String plainText(MimeMessage message) throws Exception {
    // Content-Type headers are only computed on save; sending does this too.
    message.saveChanges();
    String text = findPlainText(message.getContent());
    assertThat(text).as("text/plain part").isNotNull();
    return text;
  }

  private static String findPlainText(Object content) throws Exception {
    if (content instanceof MimeMultipart multipart) {
      for (int i = 0; i < multipart.getCount(); i++) {
        jakarta.mail.BodyPart part = multipart.getBodyPart(i);
        if (part.isMimeType("text/plain") && part.getContent() instanceof String) {
          return (String) part.getContent();
        }
        String nested = findPlainText(part.getContent());
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  @Test
  void verificationEmail_hasSubjectRecipientSenderAndLink() throws Exception {
    adapter.sendVerificationLink("brewer@example.com", "tok_ABC-123");

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Verify your BrewDeck email");
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo("brewer@example.com");
    assertThat(message.getFrom()[0].toString()).contains("no-reply@brewdeck.example");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/verify-email?token=tok_ABC-123");
  }

  @Test
  void resetEmail_linksToTheResetPage() throws Exception {
    adapter.sendResetLink("brewer@example.com", "reset_TOKEN");

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Reset your BrewDeck password");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/reset-password?token=reset_TOKEN")
        .contains("30 minutes");
  }

  @Test
  void insideATransaction_mailWaitsForCommit() {
    TransactionSynchronizationManager.initSynchronization();

    adapter.sendVerificationLink("brewer@example.com", "tok");

    verify(mailSender, never()).send(any(MimeMessage.class));
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  void rolledBackTransaction_sendsNothing() {
    TransactionSynchronizationManager.initSynchronization();

    adapter.sendResetLink("brewer@example.com", "tok");
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  void smtpFailure_isLoggedNotThrown() {
    doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

    // Must not propagate: the request already succeeded.
    adapter.sendVerificationLink("brewer@example.com", "tok");

    verify(mailSender).send(any(MimeMessage.class));
  }
}
