package com.quickbite.quickbite.notification.service.strategy;

import com.quickbite.quickbite.notification.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub push notification strategy.
 *
 * ─── HOW TO MAKE THIS PRODUCTION-READY ─────────────────────────────────────
 *
 * 1. Add Firebase Admin SDK to pom.xml:
 *    <groupId>com.google.firebase</groupId>
 *    <artifactId>firebase-admin</artifactId>
 *    <version>9.3.0</version>
 *
 * 2. Create a FirebaseConfig @Configuration bean:
 *    @Bean
 *    FirebaseApp firebaseApp() {
 *        GoogleCredentials credentials = GoogleCredentials.fromStream(
 *            new ClassPathResource("firebase-service-account.json").getInputStream());
 *        return FirebaseApp.initializeApp(FirebaseOptions.builder()
 *            .setCredentials(credentials).build());
 *    }
 *
 * 3. Add a {@code fcmToken} column to the {@code users} table (via a new
 *    Flyway migration). Clients register their device token on app startup
 *    via a dedicated endpoint: POST /api/v1/users/me/fcm-token.
 *
 * 4. Replace the log.info() call below with:
 *    Message message = Message.builder()
 *        .setToken(payload.recipient().getFcmToken())
 *        .setNotification(Notification.builder()
 *            .setTitle(payload.title())
 *            .setBody(payload.message())
 *            .build())
 *        .putData("applicationId", payload.applicationId().toString())
 *        .putData("type", payload.type().name())
 *        .build();
 *    FirebaseMessaging.getInstance().send(message);
 *
 * 5. Add push config to application.properties:
 *    quickbite.notification.push.firebase-credential=classpath:firebase-service-account.json
 *
 * ────────────────────────────────────────────────────────────────────────────
 */
@Component
public class PushNotificationStrategy implements NotificationDeliveryStrategy {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationStrategy.class);

    @Override
    public void deliver(NotificationPayload payload) {
        // TODO: Wire Firebase Cloud Messaging. See Javadoc above for full integration guide.
        log.info("[PUSH-STUB] Would send push '{}' to userId={}",
                payload.title(),
                payload.recipient().getId());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }
}
