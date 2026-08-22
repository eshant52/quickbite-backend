package com.quickbite.quickbite.notification.service.strategy;

import com.quickbite.quickbite.notification.dto.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub email notification strategy.
 *
 * ─── HOW TO MAKE THIS PRODUCTION-READY ─────────────────────────────────────
 *
 * 1. Add the AWS SES dependency to pom.xml:
 *    <groupId>software.amazon.awssdk</groupId>
 *    <artifactId>ses</artifactId>
 *
 * 2. Create a SesConfig @Configuration bean (similar to S3Config) that builds
 *    a {@code SesClient} from credentials resolved via the default chain.
 *
 * 3. Create HTML email templates (Thymeleaf or Freemarker) per notification type:
 *    - templates/email/application-submitted.html  (for admins)
 *    - templates/email/application-approved.html   (for restaurant owner)
 *    - templates/email/application-rejected.html   (for restaurant owner)
 *
 * 4. Replace the log.info() call below with:
 *    sesClient.sendEmail(SendEmailRequest.builder()
 *        .destination(d -> d.toAddresses(payload.recipient().getEmail()))
 *        .message(m -> m
 *            .subject(c -> c.data(payload.title()))
 *            .body(b -> b.html(c -> c.data(renderTemplate(payload)))))
 *        .source("noreply@quickbite.com")
 *        .build());
 *
 * 5. Add email config to application.properties:
 *    quickbite.notification.email.from=noreply@quickbite.com
 *
 * ────────────────────────────────────────────────────────────────────────────
 */
@Component
public class EmailNotificationStrategy implements NotificationDeliveryStrategy {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationStrategy.class);

    @Override
    public void deliver(NotificationPayload payload) {
        // TODO: Wire AWS SES or SMTP. See Javadoc above for full integration guide.
        log.info("[EMAIL-STUB] Would send '{}' to {} <{}>",
                payload.title(),
                payload.recipient().getName(),
                payload.recipient().getEmail());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }
}
