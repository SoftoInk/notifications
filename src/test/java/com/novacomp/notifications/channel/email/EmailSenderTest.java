package com.novacomp.notifications.channel.email;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.ProviderConfig;
import com.novacomp.notifications.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email channel")
class EmailSenderTest {

    private ProviderConfig sendGridConfig;
    private ProviderConfig mailgunConfig;

    @BeforeEach
    void setUp() {
        sendGridConfig = ProviderConfig.builder()
                .apiKey("SG.test-key")
                .property("fromEmail", "test@myapp.com")
                .property("fromName", "Test App")
                .build();

        mailgunConfig = ProviderConfig.builder()
                .apiKey("key-test123")
                .property("domain", "mg.example.com")
                .property("fromEmail", "test@mg.example.com")
                .build();
    }

    // ---------------------------------------------------------------
    // EmailNotification validation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("EmailNotification validation")
    class ValidationTests {

        @Test
        @DisplayName("valid email passes validation")
        void validEmailPasses() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("Hi")
                    .message("Body text")
                    .build();

            assertThatCode(email::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("HTML-only body passes validation")
        void htmlOnlyPasses() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("Hi")
                    .htmlBody("<p>Hello</p>")
                    .build();

            assertThatCode(email::validate).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("missing recipient fails")
        void missingRecipientFails(String recipient) {
            EmailNotification email = EmailNotification.builder()
                    .recipient(recipient)
                    .subject("Hi")
                    .message("Body")
                    .build();

            assertThatThrownBy(email::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("recipient");
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-email", "missing@tld", "@no-user.com", "spaces in@email.com"})
        @DisplayName("invalid email format fails")
        void invalidEmailFormatFails(String badEmail) {
            EmailNotification email = EmailNotification.builder()
                    .recipient(badEmail)
                    .subject("Hi")
                    .message("Body")
                    .build();

            assertThatThrownBy(email::validate)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid email");
        }

        @Test
        @DisplayName("missing subject fails")
        void missingSubjectFails() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .message("Body")
                    .build();

            assertThatThrownBy(email::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("subject");
        }

        @Test
        @DisplayName("missing both message and htmlBody fails")
        void missingBodyFails() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("Hi")
                    .build();

            assertThatThrownBy(email::validate)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("plain-text message or an HTML body");
        }

        @Test
        @DisplayName("invalid CC address fails")
        void invalidCcFails() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("Hi")
                    .message("Body")
                    .cc("bad-cc")
                    .build();

            assertThatThrownBy(email::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("cc");
        }
    }

    // ---------------------------------------------------------------
    // SendGrid sender
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("SendGridEmailSender")
    class SendGridTests {

        private SendGridEmailSender sender;

        @BeforeEach
        void setUp() {
            sender = new SendGridEmailSender(sendGridConfig);
        }

        @Test
        @DisplayName("returns provider name 'SendGrid'")
        void providerName() {
            assertThat(sender.getProviderName()).isEqualTo("SendGrid");
        }

        @Test
        @DisplayName("supported type is EmailNotification")
        void supportedType() {
            assertThat(sender.getSupportedType()).isEqualTo(EmailNotification.class);
        }

        @Test
        @DisplayName("send returns successful result with provider message ID")
        void sendReturnsSuccess() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("Test")
                    .message("Hello")
                    .build();

            NotificationResult result = sender.send(email);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getStatus()).isEqualTo(NotificationResult.Status.SENT);
            assertThat(result.getProviderName()).isEqualTo("SendGrid");
            assertThat(result.getProviderMessageId()).isNotBlank();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("send uses from field from notification when present")
        void sendUsesNotificationFrom() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .from("custom@sender.com")
                    .subject("Test")
                    .message("Hello")
                    .build();

            NotificationResult result = sender.send(email);
            assertThat(result.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("send handles attachments")
        void sendWithAttachments() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("With attachment")
                    .message("See attached")
                    .attachment(Attachment.builder()
                            .filename("test.txt")
                            .contentType("text/plain")
                            .content("hello".getBytes(StandardCharsets.UTF_8))
                            .build())
                    .build();

            NotificationResult result = sender.send(email);
            assertThat(result.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("constructor rejects null config")
        void rejectsNullConfig() {
            assertThatThrownBy(() -> new SendGridEmailSender(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("constructor rejects config without API key")
        void rejectsNullApiKey() {
            ProviderConfig noKey = ProviderConfig.builder()
                    .property("fromEmail", "test@example.com")
                    .build();

            assertThatThrownBy(() -> new SendGridEmailSender(noKey))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ---------------------------------------------------------------
    // Mailgun sender
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("MailgunEmailSender")
    class MailgunTests {

        private MailgunEmailSender sender;

        @BeforeEach
        void setUp() {
            sender = new MailgunEmailSender(mailgunConfig);
        }

        @Test
        @DisplayName("returns provider name 'Mailgun'")
        void providerName() {
            assertThat(sender.getProviderName()).isEqualTo("Mailgun");
        }

        @Test
        @DisplayName("send returns QUEUED result (Mailgun queues by default)")
        void sendReturnsQueued() {
            EmailNotification email = EmailNotification.builder()
                    .recipient("user@example.com")
                    .subject("Test")
                    .message("Hello")
                    .build();

            NotificationResult result = sender.send(email);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getStatus()).isEqualTo(NotificationResult.Status.QUEUED);
            assertThat(result.getProviderName()).isEqualTo("Mailgun");
            assertThat(result.getProviderMessageId()).contains("@mg.example.com");
        }
    }
}
