package tn.esprit.services.guardian.clients.video;

import java.util.logging.Logger;

/**
 * Agora RTC API client for interactive room sessions.
 * Location: Alternative to Daily.co for real-time audio/video in VirtualRooms.
 * API: https://api.agora.io/v1/projects/{projectId}
 * Usage: Token generation, channel management, user presence tracking.
 */
public class AgoraClient {

    private static final Logger logger = Logger.getLogger(AgoraClient.class.getName());
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

    private final String appId = getEnvOrFile("AGORA_APP_ID");
    private final String appCert = getEnvOrFile("AGORA_APP_CERT");

    public String generateAccessToken(String channelName, String userId) {
        logger.info("Generating Agora token for channel: " + channelName + " user: " + userId);
        if (appId == null || appCert == null) {
            return "Set AGORA_APP_ID and AGORA_APP_CERT environment variables.";
        }
        // TODO: Implement actual token generation
        return "agora_token_xyz";
    }

    public String trackUserPresence(String channelName) {
        logger.info("Tracking presence in channel: " + channelName);
        if (appId == null) {
            return "Set AGORA_APP_ID environment variable.";
        }
        // TODO: Implement actual Agora call
        return "3_users_online";
    }
}
