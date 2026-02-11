package com.novacomp.notifications.channel.email;

import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.NotificationSender;
import com.novacomp.notifications.ProviderConfig;
import com.novacomp.notifications.exception.DeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Simulated <a href="https://docs.sendgrid.com/api-reference/mail-send/mail-send">SendGrid v3 Mail Send</a>
 * email sender.
 *
 * <h3>Required configuration</h3>
 * <ul>
 *   <li>{@code apiKey} — SendGrid API key (starts with {@code SG.})</li>
 *   <li>{@code fromEmail} (property) — default sender address</li>
 *   <li>{@code fromName} (property, optional) — default sender display name</li>
 * </ul>
 *
 * <h3>Simulated behaviour</h3>
 * <p>Logs the equivalent {@code POST /v3/mail/send} payload and returns a
 * 202-style success result with a synthetic {@code X-Message-Id}.</p>
 */
public class SendGridEmailSender implements NotificationSender<EmailNotification> {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailSender.class);

    private final ProviderConfig config;

    public SendGridEmailSender(ProviderConfig config) {
        Objects.requireNonNull(config, "ProviderConfig must not be null");
        Objects.requireNonNull(config.getApiKey(), "SendGrid API key is required");
        this.config = config;
    }

    @Override
    public NotificationResult send(EmailNotification email) {
        String from = email.getFrom() != null ? email.getFrom()
                : config.getRequiredProperty("fromEmail");

        /*
         * Simulate: POST https://api.sendgrid.com/v3/mail/send
         * Headers:  Authorization: Bearer <apiKey>
         *           Content-Type: application/json
         * Body:
         * {
         *   "personalizations": [{ "to": [{"email": "<recipient>"}],
         *                          "cc": [...], "bcc": [...] }],
         *   "from": { "email": "<from>", "name": "<fromName>" },
         *   "subject": "<subject>",
         *   "content": [
         *     { "type": "text/plain", "value": "<message>" },
         *     { "type": "text/html",  "value": "<htmlBody>" }
         *   ],
         *   "attachments": [{ "content": "<base64>", "filename": "...", "type": "..." }]
         * }
         * Response: 202 Accepted, header X-Message-Id
         */

        log.info("[SendGrid] POST /v3/mail/send — from={}, to={}, subject=\"{}\"",
                from, email.getRecipient(), email.getSubject());

        if (!email.getCc().isEmpty()) {
            log.info("[SendGrid]   cc={}", email.getCc());
        }
        if (!email.getAttachments().isEmpty()) {
            log.info("[SendGrid]   attachments={}", email.getAttachments().size());
        }

        // Simulate successful 202 response
        String messageId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String xMessageId = messageId + ".filter0001.12345.ABCDE";

        log.info("[SendGrid] 202 Accepted — X-Message-Id: {}", xMessageId);

        return NotificationResult.success(email.getId(), getProviderName(), xMessageId);
    }

    @Override
    public Class<EmailNotification> getSupportedType() {
        return EmailNotification.class;
    }

    @Override
    public String getProviderName() {
        return "SendGrid";
    }
}
