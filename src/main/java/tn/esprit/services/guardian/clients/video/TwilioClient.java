package tn.esprit.services.guardian.clients.video;

import java.util.logging.Logger;

/**
 * Twilio Video API client for managed video sessions.
 * Location: Premium video alternative for VirtualRooms (called from "Join room").
 * API: https://video.twilio.com/v1/Rooms
 * Usage: Create rooms, generate participation tokens, manage participants.
 */
public class TwilioClient {

    private static final Logger logger = Logger.getLogger(TwilioClient.class.getName());
    private static String getEnvOrFile(String key) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) return val;
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    if (line.trim().startsWith(key + "=")) return line.substring(line.indexOf('=') + 1).trim();
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private final String accountSid = getEnvOrFile("TWILIO_ACCOUNT_SID");
    private final String authToken = getEnvOrFile("TWILIO_AUTH_TOKEN");

    public String createRoom(String roomName) {
        logger.info("Creating Twilio room: " + roomName);
        if (accountSid == null || authToken == null) {
            return "Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN environment variables.";
        }
        // TODO: Implement actual Twilio call
        return "room_sid_xyz";
    }

    public String generateParticipantToken(String roomSid, String userName) {
        logger.info("Generating token for " + userName + " to join " + roomSid);
        if (accountSid == null || authToken == null) {
            return "Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN environment variables.";
        }
        // TODO: Implement actual token generation
        return "twilio_token_xyz";
    }
}
