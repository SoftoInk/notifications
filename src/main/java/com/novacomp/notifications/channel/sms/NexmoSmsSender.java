package com.novacomp.notifications.channel.sms;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.NotificationSender;
import com.novacomp.notifications.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulated <a href="https://developer.vonage.com/en/messaging/sms/overview">
 * Vonage (formerly Nexmo) SMS API</a> sender.
 *
 * <h3>Required configuration</h3>
 * <ul>
 *   <li>{@code apiKey} — Vonage API key</li>
 *   <li>{@code apiSecret} — Vonage API secret</li>
 *   <li>{@code fromNumber} (property) — default sender number or alphanumeric ID</li>
 * </ul>
 */
public class NexmoSmsSender implements NotificationSender<SmsNotification> {

    private static final Logger log = LoggerFactory.getLogger(NexmoSmsSender.class);

    private final ProviderConfig config;

    public NexmoSmsSender(ProviderConfig config) {
        Objects.requireNonNull(config, "ProviderConfig must not be null");
        Objects.requireNonNull(config.getApiKey(), "Vonage API key is required");
        Objects.requireNonNull(config.getApiSecret(), "Vonage API secret is required");
        this.config = config;
    }

    @Override
    public NotificationResult send(SmsNotification sms) {
        String from = sms.getFrom() != null ? sms.getFrom()
                : config.getRequiredProperty("fromNumber");

        /*
         * Simulate: POST https://rest.nexmo.com/sms/json
         * Body:     api_key, api_secret, from, to, text
         * Response: 200 OK
         * {
         *   "message-count": "1",
         *   "messages": [{
         *     "to": "...",
         *     "message-id": "...",
         *     "status": "0",
         *     "remaining-balance": "...",
         *     "message-price": "...",
         *     "network": "..."
         *   }]
         * }
         */

        log.info("[Vonage] POST /sms/json — from={}, to={}", from, sms.getRecipient());

        String messageId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("[Vonage] 200 OK — message-id={}, status=0", messageId);

        return NotificationResult.success(sms.getId(), getProviderName(), messageId);
    }

    @Override
    public Class<SmsNotification> getSupportedType() {
        return SmsNotification.class;
    }

    @Override
    public String getProviderName() {
        return "Vonage (Nexmo)";
    }
}
