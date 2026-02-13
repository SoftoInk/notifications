package com.novacomp.notifications.event;

import com.novacomp.notifications.Notification;
import com.novacomp.notifications.NotificationListener;
import com.novacomp.notifications.NotificationResult;
import com.novacomp.notifications.channel.email.EmailNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DefaultNotificationEventPublisher")
class DefaultNotificationEventPublisherTest {

    DefaultNotificationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DefaultNotificationEventPublisher();
    }

    private Notification sampleNotification() {
        return EmailNotification.builder()
                .recipient("test@example.com")
                .subject("Test")
                .message("Hello")
                .build();
    }

    private NotificationResult sampleResult() {
        return NotificationResult.success("id", "TestProvider", "msg-1");
    }

    // ---------------------------------------------------------------
    // Listener invocation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("publish() notifies all registered listeners")
    void publishNotifiesAllListeners() {
        AtomicReference<NotificationResult> first = new AtomicReference<>();
        AtomicReference<NotificationResult> second = new AtomicReference<>();

        publisher.addListener((n, r) -> first.set(r));
        publisher.addListener((n, r) -> second.set(r));

        NotificationResult result = sampleResult();
        publisher.publish(sampleNotification(), result);

        assertThat(first.get()).isSameAs(result);
        assertThat(second.get()).isSameAs(result);
    }

    @Test
    @DisplayName("publish() passes both notification and result to listeners")
    void publishPassesNotificationAndResult() {
        AtomicReference<Notification> capturedNotification = new AtomicReference<>();
        AtomicReference<NotificationResult> capturedResult = new AtomicReference<>();

        publisher.addListener((n, r) -> {
            capturedNotification.set(n);
            capturedResult.set(r);
        });

        Notification notification = sampleNotification();
        NotificationResult result = sampleResult();
        publisher.publish(notification, result);

        assertThat(capturedNotification.get()).isSameAs(notification);
        assertThat(capturedResult.get()).isSameAs(result);
    }

    // ---------------------------------------------------------------
    // Failing listener isolation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("a failing listener does not prevent other listeners from being notified")
    void failingListenerDoesNotBreakOthers() {
        AtomicReference<NotificationResult> captured = new AtomicReference<>();

        publisher.addListener((n, r) -> { throw new RuntimeException("boom"); });
        publisher.addListener((n, r) -> captured.set(r));

        NotificationResult result = sampleResult();
        publisher.publish(sampleNotification(), result);

        assertThat(captured.get()).isSameAs(result);
    }

    // ---------------------------------------------------------------
    // Null rejection
    // ---------------------------------------------------------------

    @Test
    @DisplayName("publish() rejects null notification")
    void publishRejectsNullNotification() {
        assertThatThrownBy(() -> publisher.publish(null, sampleResult()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Notification");
    }

    @Test
    @DisplayName("publish() rejects null result")
    void publishRejectsNullResult() {
        assertThatThrownBy(() -> publisher.publish(sampleNotification(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("NotificationResult");
    }

    @Test
    @DisplayName("addListener() rejects null listener")
    void addListenerRejectsNull() {
        assertThatThrownBy(() -> publisher.addListener(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ---------------------------------------------------------------
    // Constructor with pre-existing listeners
    // ---------------------------------------------------------------

    @Test
    @DisplayName("constructor copies the provided listener list")
    void constructorCopiesListenerList() {
        List<NotificationListener> original = new ArrayList<>();
        AtomicReference<NotificationResult> captured = new AtomicReference<>();
        original.add((n, r) -> captured.set(r));

        DefaultNotificationEventPublisher pub = new DefaultNotificationEventPublisher(original);

        // Mutating original should not affect the publisher
        original.clear();

        NotificationResult result = sampleResult();
        pub.publish(sampleNotification(), result);

        assertThat(captured.get()).isSameAs(result);
    }

    @Test
    @DisplayName("constructor rejects null listener list")
    void constructorRejectsNullList() {
        assertThatThrownBy(() -> new DefaultNotificationEventPublisher(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ---------------------------------------------------------------
    // getListeners() immutability
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getListeners() returns an unmodifiable list")
    void getListenersIsUnmodifiable() {
        publisher.addListener((n, r) -> {});

        assertThatThrownBy(() -> publisher.getListeners().add((n, r) -> {}))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
