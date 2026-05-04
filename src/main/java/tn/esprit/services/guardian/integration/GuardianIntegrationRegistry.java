package tn.esprit.services.guardian.integration;

import java.util.List;

public final class GuardianIntegrationRegistry {

    private GuardianIntegrationRegistry() {
    }

    public static List<GuardianIntegrationItem> apiProviders() {
        return List.of(
                new GuardianIntegrationItem("OpenAI API", "AI", "Generate focus tips, summaries, and micro-quizzes.", "Focus assistant and resource summaries"),
                new GuardianIntegrationItem("Cohere API", "AI", "Text classification and semantic reranking.", "Resource tagging and search quality"),
                new GuardianIntegrationItem("Daily.co API", "Video", "Create/join lightweight live study rooms.", "Room collaboration"),
                new GuardianIntegrationItem("Agora RTC", "Video", "Real-time audio/video for group study.", "Interactive room sessions"),
                new GuardianIntegrationItem("Twilio Video", "Video", "Managed video-room infrastructure.", "Premium room sessions"),
                new GuardianIntegrationItem("Google Drive API", "Storage", "Import and attach shared files.", "Resource library ingestion"),
                new GuardianIntegrationItem("Dropbox API", "Storage", "Sync external learning folders.", "Resource synchronization"),
                new GuardianIntegrationItem("Firebase Cloud Messaging", "Notification", "Push notifications for reminders and events.", "Focus reminders"),
                new GuardianIntegrationItem("SendGrid API", "Notification", "Transactional email notifications.", "Session and digest emails"),
                new GuardianIntegrationItem("Google Calendar API", "Calendar", "Create and update study events.", "Focus planning"),
                new GuardianIntegrationItem("Microsoft Graph Calendar", "Calendar", "Outlook/Teams schedule sync.", "Enterprise calendar integration")
        );
    }

    public static List<GuardianIntegrationItem> bundles() {
        return List.of(
                new GuardianIntegrationItem("HTTP Client", "Bundle", "HTTP transport layer.", "All external REST calls"),
                new GuardianIntegrationItem("Retrofit + Jackson Converter", "Bundle", "Typed REST client serialization.", "Provider SDK wrappers"),
                new GuardianIntegrationItem("Jackson Databind", "Bundle", "JSON parsing and mapping.", "DTO mapping"),
                new GuardianIntegrationItem("Resilience4j", "Bundle", "Retry, circuit breaker, timeout.", "Fault-tolerant API calls"),
                new GuardianIntegrationItem("SLF4J + Logback", "Bundle", "Structured logging.", "Integration observability"),
                new GuardianIntegrationItem("Jakarta Validation + Hibernate Validator", "Bundle", "Bean validation.", "Input contracts"),
                new GuardianIntegrationItem("Caffeine", "Bundle", "In-memory caching.", "API response caching"),
                new GuardianIntegrationItem("Java JWT", "Bundle", "Token generation/verification.", "Session and provider auth"),
                new GuardianIntegrationItem("Java-WebSocket", "Bundle", "Realtime websocket client.", "Room events and live states")
        );
    }
}