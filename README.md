# Novacomp Notifications

## Quick Start

```java
// 1. Configure providers
ProviderConfig emailConfig = ProviderConfig.builder()
        .apiKey("SG.your-sendgrid-key")
        .property("fromEmail", "noreply@myapp.com")
        .build();

ProviderConfig smsConfig = ProviderConfig.builder()
        .apiKey("your-twilio-account-sid")
        .apiSecret("your-twilio-auth-token")
        .property("fromNumber", "+15551234567")
        .build();

ProviderConfig pushConfig = ProviderConfig.builder()
        .apiKey("your-firebase-server-key")
        .property("projectId", "myapp-12345")
        .build();

// 2. Build the service
NotificationService service = NotificationService.builder()
        .registerSender(new SendGridEmailSender(emailConfig))
        .registerSender(new TwilioSmsSender(smsConfig))
        .registerSender(new FirebasePushSender(pushConfig))
        .build();

// 3. Send notifications — same send() API regardless of channel
service.send(EmailNotification.builder()
        .recipient("user@example.com")
        .subject("Welcome!")
        .message("Thanks for signing up.")
        .build());

service.send(SmsNotification.builder()
        .recipient("+50688881234")
        .message("Your code is 483920")
        .build());

service.send(PushNotification.builder()
        .recipient("device-token")
        .title("New order")
        .message("Order #12345 confirmed!")
        .datum("orderId", "12345")
        .build());

// 4. Clean up
service.close();
```
# CUSTOM RETRY POLICY AND OBSERVERS

### NotificationService.Builder

```java
NotificationService service = NotificationService.builder()
        .registerSender(sender)              // register one or more senders
        .retryPolicy(RetryPolicy.fixed(3, Duration.ofSeconds(2)))
        .addListener((n, r) -> audit(n, r))  // optional observers
        .executorService(myExecutor)          // optional custom executor for async
        .build();
```

## Running with Docker

Build and run the examples demo without installing Java:

```bash
docker build -t novacomp-notifications .
docker run --rm novacomp-notifications
```

---

## Running Tests

```bash
# Run all tests
mvn test

# Run tests with verbose output
mvn test -Dsurefire.useFile=false

# Build the library JAR
mvn clean package
```