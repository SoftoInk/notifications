package com.novacomp.notifications.channel.push;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.ProviderConfig;
import com.novacomp.notifications.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Push notification channel")
class PushSenderTest {

    private ProviderConfig firebaseConfig;
    private ProviderConfig apnsConfig;

    @BeforeEach
    void setUp() {
        firebaseConfig = ProviderConfig.builder()
                .apiKey("fake-firebase-server-key")
                .property("projectId", "test-project")
                .build();

        apnsConfig = ProviderConfig.builder()
                .apiKey("fake-apns-key")
                .property("teamId", "TEAM123")
                .property("bundleId", "com.example.myapp")
                .build();
    }

    // ---------------------------------------------------------------
    // PushNotification validation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PushNotification validation")
    class ValidationTests {

        @Test
        @DisplayName("valid push with device token passes")
        void validWithTokenPasses() {
            PushNotification push = PushNotification.builder()
                    .recipient("some-device-token")
                    .title("Hello")
                    .message("World")
                    .build();

            assertThatCode(push::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("valid push with topic (no token) passes")
        void validWithTopicPasses() {
            PushNotification push = PushNotification.builder()
                    .topic("news")
                    .title("Breaking")
                    .message("Something happened")
                    .build();

            assertThatCode(push::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("missing both token and topic fails")
        void missingTokenAndTopicFails() {
            PushNotification push = PushNotification.builder()
                    .title("Test")
                    .message("Body")
                    .build();

            assertThatThrownBy(push::validate)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("device token")
                    .hasMessageContaining("topic");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("missing title fails")
        void missingTitleFails(String title) {
            PushNotification push = PushNotification.builder()
                    .recipient("token")
                    .title(title)
                    .message("Body")
                    .build();

            assertThatThrownBy(push::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("title");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("missing message fails")
        void missingMessageFails(String message) {
            PushNotification push = PushNotification.builder()
                    .recipient("token")
                    .title("Title")
                    .message(message)
                    .build();

            assertThatThrownBy(push::validate)
                    .isInstanceOf(ValidationException.class)
                    .extracting(e -> ((ValidationException) e).getField())
                    .isEqualTo("message");
        }

        @Test
        @DisplayName("default priority is NORMAL")
        void defaultPriorityIsNormal() {
            PushNotification push = PushNotification.builder()
                    .recipient("token")
                    .title("Title")
                    .message("Body")
                    .build();

            assertThat(push.getPriority()).isEqualTo(PushNotification.Priority.NORMAL);
        }
    }

    // ---------------------------------------------------------------
    // Firebase sender
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("FirebasePushSender")
    class FirebaseTests {

        private FirebasePushSender sender;

        @BeforeEach
        void setUp() {
            sender = new FirebasePushSender(firebaseConfig);
        }

        @Test
        @DisplayName("returns provider name 'Firebase Cloud Messaging'")
        void providerName() {
            assertThat(sender.getProviderName()).isEqualTo("Firebase Cloud Messaging");
        }

        @Test
        @DisplayName("supported type is PushNotification")
        void supportedType() {
            assertThat(sender.getSupportedType()).isEqualTo(PushNotification.class);
        }

        @Test
        @DisplayName("send returns SENT result with FCM message name")
        void sendReturnsSent() {
            PushNotification push = PushNotification.builder()
                    .recipient("device-token-abc")
                    .title("Order Update")
                    .message("Your order is ready!")
                    .datum("orderId", "ORD-42")
                    .priority(PushNotification.Priority.HIGH)
                    .build();

            NotificationResult result = sender.send(push);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getStatus()).isEqualTo(NotificationResult.Status.SENT);
            assertThat(result.getProviderName()).isEqualTo("Firebase Cloud Messaging");
            assertThat(result.getProviderMessageId()).startsWith("projects/test-project/messages/");
        }

        @Test
        @DisplayName("send uses default project ID when not configured")
        void sendsWithDefaultProjectId() {
            ProviderConfig noProject = ProviderConfig.builder()
                    .apiKey("key")
                    .build();
            FirebasePushSender senderNoProject = new FirebasePushSender(noProject);

            PushNotification push = PushNotification.builder()
                    .recipient("token")
                    .title("Test")
                    .message("Body")
                    .build();

            NotificationResult result = senderNoProject.send(push);
            assertThat(result.getProviderMessageId()).startsWith("projects/default-project/messages/");
        }
    }

    // ---------------------------------------------------------------
    // APNs sender
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ApnsPushSender")
    class ApnsTests {

        private ApnsPushSender sender;

        @BeforeEach
        void setUp() {
            sender = new ApnsPushSender(apnsConfig);
        }

        @Test
        @DisplayName("returns provider name containing 'Apple'")
        void providerName() {
            assertThat(sender.getProviderName()).contains("Apple");
        }

        @Test
        @DisplayName("send returns SENT result with APNs UUID")
        void sendReturnsSent() {
            PushNotification push = PushNotification.builder()
                    .recipient("apns-device-token-xyz")
                    .title("New Message")
                    .message("You have a new message")
                    .priority(PushNotification.Priority.HIGH)
                    .build();

            NotificationResult result = sender.send(push);

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getProviderName()).contains("Apple");
            assertThat(result.getProviderMessageId()).isNotBlank();
        }

        @Test
        @DisplayName("constructor rejects config without bundleId")
        void rejectsNoBundleId() {
            ProviderConfig noBundleId = ProviderConfig.builder()
                    .apiKey("key")
                    .property("teamId", "TEAM123")
                    .build();

            ApnsPushSender senderNoBundleId = new ApnsPushSender(noBundleId);

            PushNotification push = PushNotification.builder()
                    .recipient("token")
                    .title("Test")
                    .message("Body")
                    .build();

            assertThatThrownBy(() -> senderNoBundleId.send(push))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bundleId");
        }
    }
}
