package com.novacomp.notifications.channel.sms;

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

import static org.assertj.core.api.Assertions.*;

@DisplayName("SMS channel")
class SmsSenderTest {

    private ProviderConfig twilioConfig;
    private ProviderConfig nexmoConfig;

    @BeforeEach
    void setUp() {
        twilioConfig = ProviderConfig.builder()
                .apiKey("AC_test_sid")
                .apiSecret("test_auth_token")
                .property("fromNumber", "+15551234567")
                .build();

        nexmoConfig = ProviderConfig.builder()
                .apiKey("nexmo-api-key")
                .apiSecret("nexmo-api-secret")
                .property("fromNumber", "+15559876543")
                .build();
    }

    // ---------------------------------------------------------------
    // SmsNotification validation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("SmsNotification validation")
    class ValidationTests {

        @Test
        @DisplayName("valid SMS passes validation")
        void validSmsPasses() {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("+50688881234")
                    .message("Hello")
                    .build();

            assertThatCode(sms::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("phone number without + prefix passes")
        void phoneWithoutPlusPasses() {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("50688881234")
                    .message("Hello")
                    .build();

            assertThatCode(sms::validate).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("missing recipient fails")
        void missingRecipientFails(String recipient) {
            SmsNotification sms = SmsNotification.builder()
                    .recipient(recipient)
                    .message("Hello")
                    .build();

            assertThatThrownBy(sms::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("recipient");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "123", "+0999", "++12345678"})
        @DisplayName("invalid phone number fails")
        void invalidPhoneFails(String phone) {
            SmsNotification sms = SmsNotification.builder()
                    .recipient(phone)
                    .message("Hello")
                    .build();

            assertThatThrownBy(sms::validate)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid phone number");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("missing message fails")
        void missingMessageFails(String message) {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("+50688881234")
                    .message(message)
                    .build();

            assertThatThrownBy(sms::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("message");
        }

        @Test
        @DisplayName("message exceeding 1600 chars fails")
        void messageTooLongFails() {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("+50688881234")
                    .message("x".repeat(1601))
                    .build();

            assertThatThrownBy(sms::validate)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("1600 character limit");
        }
    }

    // ---------------------------------------------------------------
    // Twilio sender
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("TwilioSmsSender")
    class TwilioTests {

        private TwilioSmsSender sender;

        @BeforeEach
        void setUp() {
            sender = new TwilioSmsSender(twilioConfig);
        }

        @Test
        @DisplayName("returns provider name 'Twilio'")
        void providerName() {
            assertThat(sender.getProviderName()).isEqualTo("Twilio");
        }

        @Test
        @DisplayName("supported type is SmsNotification")
        void supportedType() {
            assertThat(sender.getSupportedType()).isEqualTo(SmsNotification.class);
        }

        @Test
        @DisplayName("send returns QUEUED result with Twilio SID")
        void sendReturnsQueued() {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("+50688881234")
                    .message("Verification code: 1234")
                    .build();

            NotificationResult result = sender.send(sms);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getStatus()).isEqualTo(NotificationResult.Status.QUEUED);
            assertThat(result.getProviderName()).isEqualTo("Twilio");
            assertThat(result.getProviderMessageId()).startsWith("SM");
        }

        @Test
        @DisplayName("send uses 'from' from notification when present")
        void sendUsesNotificationFrom() {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("+50688881234")
                    .message("Hello")
                    .from("+15559999999")
                    .build();

            NotificationResult result = sender.send(sms);
            assertThat(result.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("constructor rejects config without API secret")
        void rejectsNoApiSecret() {
            ProviderConfig noSecret = ProviderConfig.builder()
                    .apiKey("AC_test_sid")
                    .property("fromNumber", "+15551234567")
                    .build();

            assertThatThrownBy(() -> new TwilioSmsSender(noSecret))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ---------------------------------------------------------------
    // Nexmo / Vonage sender
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("NexmoSmsSender")
    class NexmoTests {

        private NexmoSmsSender sender;

        @BeforeEach
        void setUp() {
            sender = new NexmoSmsSender(nexmoConfig);
        }

        @Test
        @DisplayName("returns provider name containing 'Vonage'")
        void providerName() {
            assertThat(sender.getProviderName()).contains("Vonage");
        }

        @Test
        @DisplayName("send returns SENT result")
        void sendReturnsSent() {
            SmsNotification sms = SmsNotification.builder()
                    .recipient("+50688881234")
                    .message("Hello from Vonage")
                    .build();

            NotificationResult result = sender.send(sms);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getStatus()).isEqualTo(NotificationResult.Status.SENT);
        }
    }
}
