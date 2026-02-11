package com.novacomp.notifications.channel.push;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.NotificationSender;
import com.novacomp.notifications.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulated <a href="https://developer.apple.com/documentation/usernotifications/sending-notification-requests-to-apns">
 * Apple Push Notification service (APNs) HTTP/2 API</a> sender.
 *
 * <h3>Required configuration</h3>
 * <ul>
 *   <li>{@code apiKey} — APNs authentication key (.p8 key ID or token)</li>
 *   <li>{@code teamId} (property) — Apple Developer Team ID</li>
 *   <li>{@code bundleId} (property) — App bundle identifier (e.g. {@code com.example.myapp})</li>
 * </ul>
 */
public class ApnsPushSender implements NotificationSender<PushNotification> {

    private static final Logger log = LoggerFactory.getLogger(ApnsPushSender.class);

    private final ProviderConfig config;

    public ApnsPushSender(ProviderConfig config) {
        Objects.requireNonNull(config, "ProviderConfig must not be null");
        Objects.requireNonNull(config.getApiKey(), "APNs auth key is required");
        this.config = config;
    }

    @Override
    public NotificationResult send(PushNotification push) {
        String bundleId = config.getRequiredProperty("bundleId");

        /*
         * Simulate: POST https://api.push.apple.com/3/device/{deviceToken}
         * Headers:
         *   authorization: bearer {JWT}
         *   apns-topic: {bundleId}
         *   apns-priority: 10 (HIGH) | 5 (NORMAL)
         *   apns-push-type: alert
         * Body:
         * {
         *   "aps": {
         *     "alert": { "title": "<title>", "body": "<message>" },
         *     "sound": "default",
         *     "mutable-content": 1
         *   },
         *   "<custom-key>": "<custom-value>"
         * }
         * Response: 200 OK, header apns-id: <uuid>
         */

        int apnsPriority = push.getPriority() == PushNotification.Priority.HIGH ? 10 : 5;

        log.info("[APNs] POST /3/device/{} — topic={}, priority={}",
                truncate(push.getRecipient(), 12), bundleId, apnsPriority);
        log.info("[APNs]   alert: title=\"{}\", body=\"{}\"",
                push.getTitle(), truncate(push.getMessage(), 40));

        String apnsId = UUID.randomUUID().toString();

        log.info("[APNs] 200 OK — apns-id={}", apnsId);

        return NotificationResult.success(push.getId(), getProviderName(), apnsId);
    }

    @Override
    public Class<PushNotification> getSupportedType() {
        return PushNotification.class;
    }

    @Override
    public String getProviderName() {
        return "Apple Push Notification service";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "null";
        return value.length() > maxLength
                ? value.substring(0, maxLength) + "…"
                : value;
    }
}
