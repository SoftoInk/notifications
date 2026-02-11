package com.novacomp.notifications.channel.sms;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.NotificationSender;
import com.novacomp.notifications.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulated <a href="https://www.twilio.com/docs/sms/api/message-resource#create-a-message-resource">
 * Twilio Messages API</a> SMS sender.
 *
 * <h3>Required configuration</h3>
 * <ul>
 *   <li>{@code apiKey} — Twilio Account SID</li>
 *   <li>{@code apiSecret} — Twilio Auth Token</li>
 *   <li>{@code fromNumber} (property) — default sender phone number</li>
 * </ul>
 */
public class TwilioSmsSender implements NotificationSender<SmsNotification> {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final ProviderConfig config;

    public TwilioSmsSender(ProviderConfig config) {
        Objects.requireNonNull(config, "ProviderConfig must not be null");
        Objects.requireNonNull(config.getApiKey(), "Twilio Account SID (apiKey) is required");
        Objects.requireNonNull(config.getApiSecret(), "Twilio Auth Token (apiSecret) is required");
        this.config = config;
    }

    @Override
    public NotificationResult send(SmsNotification sms) {
        String from = sms.getFrom() != null ? sms.getFrom()
                : config.getRequiredProperty("fromNumber");
        String accountSid = config.getApiKey();

        /*
         * Simulate: POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json
         * Auth:     Basic {AccountSid}:{AuthToken}
         * Form:     To=<recipient>&From=<from>&Body=<message>
         * Response: 201 Created
         * {
         *   "sid": "SMxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
         *   "status": "queued",
         *   "to": "+506...",
         *   "from": "+1555...",
         *   "body": "...",
         *   "date_created": "..."
         * }
         */

        log.info("[Twilio] POST /2010-04-01/Accounts/{}/Messages.json — from={}, to={}",
                accountSid, from, sms.getRecipient());
        log.info("[Twilio]   body=\"{}\"",
                sms.getMessage().length() > 50
                        ? sms.getMessage().substring(0, 50) + "…"
                        : sms.getMessage());

        String sid = "SM" + UUID.randomUUID().toString().replace("-", "");

        log.info("[Twilio] 201 Created — sid={}, status=queued", sid);

        return NotificationResult.queued(sms.getId(), getProviderName(), sid);
    }

    @Override
    public Class<SmsNotification> getSupportedType() {
        return SmsNotification.class;
    }

    @Override
    public String getProviderName() {
        return "Twilio";
    }
}
