package tn.esprit.services.guardian.clients.video;

import java.util.logging.Logger;

/**
 * Daily.co API client for lightweight video rooms.
 * Location: Called when user clicks "Join room" on a VirtualRoom card.
 * API: https://api.daily.co/v1/rooms
 * Usage: Create session links, manage room lifecycle, get room state.
 */
public class DailyCoClient {

    private static final Logger logger = Logger.getLogger(DailyCoClient.class.getName());
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

    private final String apiKey = getEnvOrFile("DAILYCO_API_KEY");

    public String createRoomSession(String roomName, int maxParticipants) {
        logger.info("Creating Daily.co room: " + roomName);
        if (apiKey == null) {
            return "Set DAILYCO_API_KEY environment variable to enable video rooms.";
        }
        // TODO: Implement actual Daily.co call
        return "https://daily.co/" + roomName + "/join";
    }

    public String getRoomStatus(String roomId) {
        logger.info("Checking room status: " + roomId);
        if (apiKey == null) {
            return "Set DAILYCO_API_KEY environment variable.";
        }
        // TODO: Implement actual Daily.co call
        return "room_active";
    }
}
