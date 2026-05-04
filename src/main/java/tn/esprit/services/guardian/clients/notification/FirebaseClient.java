package tn.esprit.services.guardian.clients.notification;

import java.util.logging.Logger;

/**
 * Firebase Cloud Messaging client for push notifications.
 * Location: Called to send focus reminders, room invites, and session alerts.
 * API: https://fcm.googleapis.com/fcm/send
 * Usage: Send notifications to mobile/desktop, track delivery, handle subscriptions.
 */
public class FirebaseClient {

    private static final Logger logger = Logger.getLogger(FirebaseClient.class.getName());
    private final String serverKey = System.getenv("FIREBASE_SERVER_KEY");

    public String sendFocusReminder(String userId, String sessionId) {
        logger.info("Sending focus reminder to user: " + userId + " for session: " + sessionId);
        if (serverKey == null) {
            return "Set FIREBASE_SERVER_KEY environment variable.";
        }
        // TODO: Implement actual Firebase call
        return "notification_sent";
    }

    public String sendRoomInvite(String userId, String roomId) {
        logger.info("Sending room invite to user: " + userId + " for room: " + roomId);
        if (serverKey == null) {
            return "Set FIREBASE_SERVER_KEY environment variable.";
        }
        // TODO: Implement actual Firebase call
        return "invite_sent";
    }
}
