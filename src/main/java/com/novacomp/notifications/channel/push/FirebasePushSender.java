package com.novacomp.notifications.channel.push;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.NotificationSender;
import com.novacomp.notifications.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulated <a href="https://firebase.google.com/docs/cloud-messaging/send-message">
 * Firebase Cloud Messaging (FCM) HTTP v1 API</a> push sender.
 *
 * <h3>Required configuration</h3>
 * <ul>
 *   <li>{@code apiKey} — Firebase server key or service-account credentials</li>
 *   <li>{@code projectId} (property) — Google Cloud project ID</li>
 * </ul>
 */
public class FirebasePushSender implements NotificationSender<PushNotification> {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    private final ProviderConfig config;

    public FirebasePushSender(ProviderConfig config) {
        Objects.requireNonNull(config, "ProviderConfig must not be null");
        Objects.requireNonNull(config.getApiKey(), "Firebase server key / credentials are required");
        this.config = config;
    }

    @Override
    public NotificationResult send(PushNotification push) {
        String projectId = config.getProperty("projectId") != null
                ? config.getProperty("projectId") : "default-project";

        /*
         * Simulate: POST https://fcm.googleapis.com/v1/projects/{projectId}/messages:send
         * Auth:     Bearer {OAuth2 access token derived from service account}
         * Body:
         * {
         *   "message": {
         *     "token": "<recipient>",          // or "topic": "<topic>"
         *     "notification": {
         *       "title": "<title>",
         *       "body":  "<message>",
         *       "image": "<imageUrl>"
         *     },
         *     "data": { ... },
         *     "android": { "priority": "high" | "normal" }
         *   }
         * }
         * Response: 200 OK
         * { "name": "projects/{projectId}/messages/{messageId}" }
         */

        String target = push.getRecipient() != null
                ? "token=" + truncate(push.getRecipient(), 12)
                : "topic=" + push.getTopic();

        log.info("[FCM] POST /v1/projects/{}/messages:send — {}, title=\"{}\"",
                projectId, target, push.getTitle());

        if (push.getData() != null && !push.getData().isEmpty()) {
            log.info("[FCM]   data keys={}", push.getData().keySet());
        }

        String messageId = UUID.randomUUID().toString().substring(0, 16);
        String name = "projects/" + projectId + "/messages/" + messageId;

        log.info("[FCM] 200 OK — name={}", name);

        return NotificationResult.success(push.getId(), getProviderName(), name);
    }

    @Override
    public Class<PushNotification> getSupportedType() {
        return PushNotification.class;
    }

    @Override
    public String getProviderName() {
        return "Firebase Cloud Messaging";
    }

    private static String truncate(String value, int maxLength) {
        return value.length() > maxLength
                ? value.substring(0, maxLength) + "…"
                : value;
    }
}
