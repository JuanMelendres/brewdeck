package com.brewdeck.brewdeck_api.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brewdeck.brewdeck_api.auth.reset.MailProperties;
import com.brewdeck.brewdeck_api.common.i18n.TestMessages;
import com.brewdeck.brewdeck_api.common.metrics.BrewdeckMetrics;
import com.brewdeck.brewdeck_api.featureflag.FeatureFlagService;
import com.brewdeck.brewdeck_api.featureflag.FeatureKeys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SmtpMailAdapterTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final BrewdeckMetrics metrics = new BrewdeckMetrics(meterRegistry);

  private final JavaMailSender mailSender = mock(JavaMailSender.class);
  private final FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
  private final SmtpMailAdapter adapter =
      new SmtpMailAdapter(
          mailSender,
          new MailProperties(
              true, "https://brewdeck.example", "BrewDeck <no-reply@brewdeck.example>"),
          new SyncTaskExecutor(),
          TestMessages.messageSource(),
          featureFlagService,
          metrics);

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

  private SmtpMailAdapter adapterWithFrom(String from) {
    return new SmtpMailAdapter(
        mailSender,
        new MailProperties(true, "https://brewdeck.example", from),
        new SyncTaskExecutor(),
        TestMessages.messageSource(),
        featureFlagService,
        metrics);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"BrewDeck no-reply@brewdeck.example", "not-an-address", "   "})
  void constructor_rejectsAnInvalidSender(String from) {
    assertThatThrownBy(() -> adapterWithFrom(from))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BREWDECK_MAIL_FROM");
  }

  @ParameterizedTest
  @ValueSource(strings = {"BrewDeck <no-reply@brewdeck.example>", "no-reply@brewdeck.example"})
  void constructor_acceptsAValidSender(String from) {
    assertThatCode(() -> adapterWithFrom(from)).doesNotThrowAnyException();
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
    adapter.sendVerificationLink("brewer@example.com", "tok_ABC-123", Locale.ENGLISH);

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Verify your BrewDeck email");
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo("brewer@example.com");
    assertThat(message.getFrom()[0].toString()).contains("no-reply@brewdeck.example");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/verify-email?token=tok_ABC-123");
  }

  @Test
  void accountExistsNotice_pointsToLogin_andCarriesNoToken() throws Exception {
    adapter.sendAccountExistsNotice("brewer@example.com", Locale.ENGLISH);

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("You already have a BrewDeck account");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/login")
        .contains("Forgot password?")
        .doesNotContain("token=");
  }

  @Test
  void accountExistsNotice_inSpanish() throws Exception {
    adapter.sendAccountExistsNotice("brewer@example.com", Locale.forLanguageTag("es"));

    assertThat(sentMessage().getSubject()).isEqualTo("Ya tienes una cuenta en BrewDeck");
  }

  @Test
  void resetEmail_linksToTheResetPage() throws Exception {
    adapter.sendResetLink("brewer@example.com", "reset_TOKEN", Locale.ENGLISH);

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Reset your BrewDeck password");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/reset-password?token=reset_TOKEN")
        .contains("30 minutes");
  }

  @Test
  void insideATransaction_mailWaitsForCommit() {
    TransactionSynchronizationManager.initSynchronization();

    adapter.sendVerificationLink("brewer@example.com", "tok", Locale.ENGLISH);

    verify(mailSender, never()).send(any(MimeMessage.class));
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  void rolledBackTransaction_sendsNothing() {
    TransactionSynchronizationManager.initSynchronization();

    adapter.sendResetLink("brewer@example.com", "tok", Locale.ENGLISH);
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    verify(mailSender, never()).send(any(MimeMessage.class));
  }

  @Test
  void smtpFailure_isLoggedNotThrown() {
    doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

    // Must not propagate: the request already succeeded.
    adapter.sendVerificationLink("brewer@example.com", "tok", Locale.ENGLISH);

    verify(mailSender).send(any(MimeMessage.class));
    assertThat(
            meterRegistry
                .counter("brewdeck.mail.sent", "kind", "verification", "outcome", "failure")
                .count())
        .isEqualTo(1);
  }

  @Test
  void sentEmail_isCountedByKind() {
    adapter.sendResetLink("brewer@example.com", "tok", Locale.ENGLISH);

    assertThat(
            meterRegistry
                .counter("brewdeck.mail.sent", "kind", "password-reset", "outcome", "success")
                .count())
        .isEqualTo(1);
  }

  @Test
  void spanishUser_getsTheEmailInSpanishWithAccentsIntact() throws Exception {
    adapter.sendResetLink("brewer@example.com", "tok_ES", Locale.forLanguageTag("es"));

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Restablece tu contraseña de BrewDeck");
    assertThat(plainText(message))
        .contains("Elige una nueva aquí (válido por 30 minutos):")
        .contains("https://brewdeck.example/reset-password?token=tok_ES")
        .contains("Si no fuiste tú, ignora este correo.");
  }

  @Test
  void emailChangeConfirm_linksToTheConfirmPage() throws Exception {
    adapter.sendConfirmLink("new@example.com", "confirm_TOKEN", Locale.ENGLISH);

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Confirm your new BrewDeck email");
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo("new@example.com");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/email-change/confirm?token=confirm_TOKEN")
        .contains("valid for 1 hour");
  }

  @Test
  void emailChangeRequested_namesTheMaskedAddress_andLinksToCancel() throws Exception {
    adapter.sendChangeRequestedNotice(
        "old@example.com", "n***@example.com", "cancel_TOKEN", Locale.forLanguageTag("es"));

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Tu correo de BrewDeck está por cambiar");
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo("old@example.com");
    assertThat(plainText(message))
        .contains("n***@example.com")
        .contains("https://brewdeck.example/email-change/cancel?token=cancel_TOKEN");
  }

  @Test
  void emailChangeAddressTaken_pointsToLogin_andCarriesNoToken() throws Exception {
    adapter.sendAddressTakenNotice("taken@example.com", Locale.ENGLISH);

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("You already have a BrewDeck account");
    assertThat(plainText(message))
        .contains("https://brewdeck.example/login")
        .doesNotContain("token=");
  }

  @Test
  void retentionReminder_namesTheDeletionDate_andLinksToVerify() throws Exception {
    adapter.sendRetentionReminder(
        "late@example.com",
        "remind_TOKEN",
        Instant.parse("2026-10-27T08:00:00Z"),
        Locale.forLanguageTag("es"));

    MimeMessage message = sentMessage();
    assertThat(message.getSubject())
        .isEqualTo("Verifica tu correo de BrewDeck o la cuenta se eliminará");
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo("late@example.com");
    assertThat(plainText(message))
        .contains("se eliminará a partir del 27 de octubre de 2026")
        .contains("https://brewdeck.example/verify-email?token=remind_TOKEN");
  }

  @Test
  void emailChanged_hasNoLink() throws Exception {
    adapter.sendEmailChangedNotice("old@example.com", "n***@example.com", Locale.ENGLISH);

    MimeMessage message = sentMessage();
    assertThat(message.getSubject()).isEqualTo("Your BrewDeck email was changed");
    assertThat(plainText(message)).contains("now uses n***@example.com").doesNotContain("https://");
  }

  @Test
  void emails_linkThePrivacyNotice_onlyOnceTheTextsArePublished() throws Exception {
    adapter.sendResetLink("brewer@example.com", "reset_TOKEN", Locale.ENGLISH);
    assertThat(plainText(sentMessage())).doesNotContain("/privacy");

    org.mockito.Mockito.reset(mailSender);
    realMimeMessages();
    when(featureFlagService.isEnabled(FeatureKeys.LEGAL_TERMS)).thenReturn(true);
    adapter.sendResetLink("brewer@example.com", "reset_TOKEN", Locale.ENGLISH);
    assertThat(plainText(sentMessage()))
        .contains("Privacy notice: https://brewdeck.example/privacy");
  }
}
