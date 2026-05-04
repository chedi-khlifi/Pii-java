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
    private final String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    private final String authToken = "AIzaSyD09mMWjb7EqPAgKDmtbR5vS5ndvwMNrC8";

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
        if (accountSid == null) {
            return "Set TWILIO_ACCOUNT_SID environment variable.";
        }
        // TODO: Implement actual token generation
        return "twilio_token_xyz";
    }
}
