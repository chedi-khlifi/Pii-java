package tn.esprit.services.guardian.clients.notification;

import java.util.logging.Logger;

/**
 * SendGrid API client for transactional email notifications.
 * Location: Called for email digests, session reports, and team summaries.
 * API: https://api.sendgrid.com/v3/mail/send
 * Usage: Send focus session reports, resource digest emails, team notifications.
 */
public class SendGridClient {

    private static final Logger logger = Logger.getLogger(SendGridClient.class.getName());
    private final String apiKey = System.getenv("SENDGRID_API_KEY");

    public String sendSessionReport(String userEmail, String sessionId) {
        logger.info("Sending session report to: " + userEmail);
        if (apiKey == null) {
            return "Set SENDGRID_API_KEY environment variable.";
        }
        // TODO: Implement actual SendGrid call
        return "email_sent";
    }

    public String sendWeeklyDigest(String userEmail) {
        logger.info("Sending weekly digest to: " + userEmail);
        if (apiKey == null) {
            return "Set SENDGRID_API_KEY environment variable.";
        }
        // TODO: Implement actual SendGrid call
        return "digest_sent";
    }
}
