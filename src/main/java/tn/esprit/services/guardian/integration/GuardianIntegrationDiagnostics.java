package tn.esprit.services.guardian.integration;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

public class GuardianIntegrationDiagnostics {

    private static final Logger logger = Logger.getLogger(GuardianIntegrationDiagnostics.class.getName());

    public static Map<String, IntegrationStatus> checkAllIntegrations() {
        Map<String, IntegrationStatus> status = new HashMap<>();

        checkBundles(status);
        checkApis(status);

        return status;
    }

    private static void checkBundles(Map<String, IntegrationStatus> status) {
        status.put("HTTP Client", checkHttpClient());
        status.put("Retrofit + Jackson", checkRetrofit());
        status.put("Jackson Databind", checkJackson());
        status.put("Resilience4j", checkResilience4j());
        status.put("SLF4J + Logback", checkLogging());
        status.put("Jakarta Validation", checkValidation());
        status.put("Caffeine", checkCaffeine());
        status.put("Java JWT", checkJwt());
        status.put("Java-WebSocket", checkWebSocket());
    }

    private static void checkApis(Map<String, IntegrationStatus> status) {
        status.put("OpenAI API", checkOpenAi());
        status.put("Cohere API", checkCohere());
        status.put("Daily.co API", checkDailyco());
        status.put("Agora RTC", checkAgora());
        status.put("Twilio Video", checkTwilio());
        status.put("Google Drive API", checkGoogleDrive());
        status.put("Dropbox API", checkDropbox());
        status.put("Firebase Cloud Messaging", checkFirebase());
        status.put("SendGrid API", checkSendGrid());
        status.put("Google Calendar API", checkGoogleCalendar());
        status.put("Microsoft Graph Calendar", checkMicrosoftGraph());
    }

    private static IntegrationStatus checkHttpClient() {
        try {
            Class.forName("java.net.http.HttpClient");
            return new IntegrationStatus("HTTP Client", true, "JDK HTTP client loaded", "src/main/java/tn/esprit/services/guardian/clients/HttpClientProvider.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("HTTP Client", false, "JDK HTTP client unavailable", "src/main/java/tn/esprit/services/guardian/clients/HttpClientProvider.java");
        }
    }

    private static IntegrationStatus checkRetrofit() {
        try {
            Class.forName("retrofit2.Retrofit");
            return new IntegrationStatus("Retrofit", true, "Retrofit framework loaded", "src/main/java/tn/esprit/services/guardian/clients/RetrofitProvider.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Retrofit", false, "Dependency missing", "pom.xml");
        }
    }

    private static IntegrationStatus checkJackson() {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return new IntegrationStatus("Jackson", true, "JSON serialization ready", "src/main/java/tn/esprit/services/guardian/clients/JsonMapperProvider.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Jackson", false, "Dependency missing", "pom.xml");
        }
    }

    private static IntegrationStatus checkResilience4j() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.ResiliencePolicy");
            return new IntegrationStatus("Resilience4j", true, "Fault tolerance ready (retry/circuitbreaker/timeout)", "src/main/java/tn/esprit/services/guardian/clients/ResiliencePolicy.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Resilience4j", false, "Dependency missing", "src/main/java/tn/esprit/services/guardian/clients/ResiliencePolicy.java");
        }
    }

    private static IntegrationStatus checkLogging() {
        try {
            Class.forName("ch.qos.logback.classic.Logger");
            return new IntegrationStatus("SLF4J+Logback", true, "Logging configured", "src/main/resources/logback.xml");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("SLF4J+Logback", false, "Dependency missing", "pom.xml");
        }
    }

    private static IntegrationStatus checkValidation() {
        try {
            Class.forName("org.hibernate.validator.HibernateValidator");
            return new IntegrationStatus("Jakarta Validation", true, "Input validation ready", "src/main/java/tn/esprit/services/guardian/dto/ValidatedDto.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Jakarta Validation", false, "Dependency missing", "pom.xml");
        }
    }

    private static IntegrationStatus checkCaffeine() {
        try {
            Class.forName("com.github.benmanes.caffeine.cache.Cache");
            return new IntegrationStatus("Caffeine", true, "Caching ready", "src/main/java/tn/esprit/services/guardian/cache/CacheProvider.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Caffeine", false, "Dependency missing", "pom.xml");
        }
    }

    private static IntegrationStatus checkJwt() {
        try {
            Class.forName("tn.esprit.services.guardian.auth.TokenManager");
            return new IntegrationStatus("TokenManager", true, "Token auth ready", "src/main/java/tn/esprit/services/guardian/auth/TokenManager.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("TokenManager", false, "Class missing", "src/main/java/tn/esprit/services/guardian/auth/TokenManager.java");
        }
    }

    private static IntegrationStatus checkWebSocket() {
        try {
            Class.forName("tn.esprit.services.guardian.realtime.RoomWebSocketClient");
            return new IntegrationStatus("Java-WebSocket", true, "Realtime events ready", "src/main/java/tn/esprit/services/guardian/realtime/RoomWebSocketClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Java-WebSocket", false, "Dependency missing", "src/main/java/tn/esprit/services/guardian/realtime/RoomWebSocketClient.java");
        }
    }

    private static IntegrationStatus checkOpenAi() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.ai.OpenAiClient");
            return new IntegrationStatus("OpenAI API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/ai/OpenAiClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("OpenAI API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/ai/OpenAiClient.java");
        }
    }

    private static IntegrationStatus checkCohere() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.ai.CohereClient");
            return new IntegrationStatus("Cohere API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/ai/CohereClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Cohere API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/ai/CohereClient.java");
        }
    }

    private static IntegrationStatus checkDailyco() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.video.DailyCoClient");
            return new IntegrationStatus("Daily.co API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/video/DailyCoClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Daily.co API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/video/DailyCoClient.java");
        }
    }

    private static IntegrationStatus checkAgora() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.video.AgoraClient");
            return new IntegrationStatus("Agora RTC", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/video/AgoraClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Agora RTC", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/video/AgoraClient.java");
        }
    }

    private static IntegrationStatus checkTwilio() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.video.TwilioClient");
            return new IntegrationStatus("Twilio Video", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/video/TwilioClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Twilio Video", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/video/TwilioClient.java");
        }
    }

    private static IntegrationStatus checkGoogleDrive() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.storage.GoogleDriveClient");
            return new IntegrationStatus("Google Drive API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/storage/GoogleDriveClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Google Drive API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/storage/GoogleDriveClient.java");
        }
    }

    private static IntegrationStatus checkDropbox() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.storage.DropboxClient");
            return new IntegrationStatus("Dropbox API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/storage/DropboxClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Dropbox API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/storage/DropboxClient.java");
        }
    }

    private static IntegrationStatus checkFirebase() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.notification.FirebaseClient");
            return new IntegrationStatus("Firebase Cloud Messaging", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/notification/FirebaseClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Firebase Cloud Messaging", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/notification/FirebaseClient.java");
        }
    }

    private static IntegrationStatus checkSendGrid() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.notification.SendGridClient");
            return new IntegrationStatus("SendGrid API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/notification/SendGridClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("SendGrid API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/notification/SendGridClient.java");
        }
    }

    private static IntegrationStatus checkGoogleCalendar() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.calendar.GoogleCalendarClient");
            return new IntegrationStatus("Google Calendar API", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/calendar/GoogleCalendarClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Google Calendar API", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/calendar/GoogleCalendarClient.java");
        }
    }

    private static IntegrationStatus checkMicrosoftGraph() {
        try {
            Class.forName("tn.esprit.services.guardian.clients.calendar.MicrosoftGraphClient");
            return new IntegrationStatus("Microsoft Graph Calendar", true, "Client loaded", "src/main/java/tn/esprit/services/guardian/clients/calendar/MicrosoftGraphClient.java");
        } catch (ClassNotFoundException e) {
            return new IntegrationStatus("Microsoft Graph Calendar", false, "Class missing", "src/main/java/tn/esprit/services/guardian/clients/calendar/MicrosoftGraphClient.java");
        }
    }

    public static class IntegrationStatus {
        public String name;
        public boolean ready;
        public String status;
        public String location;

        public IntegrationStatus(String name, boolean ready, String status, String location) {
            this.name = name;
            this.ready = ready;
            this.status = status;
            this.location = location;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s (@ %s)", ready ? "✓" : "⊘", name, status, location);
        }
    }
}
