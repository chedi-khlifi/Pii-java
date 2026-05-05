package tn.esprit.services.guardian.clients.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI API client for focus tips and content summaries.
 * Location: Called when user clicks "AI Focus Tips" button in Focus Timer view.
 * API: https://api.openai.com/v1/chat/completions
 * Usage: Generate personalized focus advice, summarize resources, create quick quizzes.
 */
public class OpenAiClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);
    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private final HttpClient httpClient;

    public OpenAiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "Error: OPENAI_API_KEY environment variable is not set. Please set it to enable AI features.";
        }

        try {
            JSONObject root = new JSONObject();
            root.put("model", "gpt-3.5-turbo");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemPrompt));
            messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userPrompt));
            root.put("messages", messages);

            String requestBody = root.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject responseNode = new JSONObject(response.body());
                JSONArray choices = responseNode.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                    if (message != null) {
                        return message.optString("content", "");
                    }
                }
                return "";
            } else {
                logger.error("OpenAI API returned status {}: {}", response.statusCode(), response.body());
                return "Error from AI Service: " + response.statusCode();
            }
        } catch (Exception e) {
            logger.error("Failed to call OpenAI API", e);
            return "Network Error: Could not connect to AI Service.";
        }
    }

    public String generateFocusTips(String userContext) {
        logger.info("Generating focus tips for context: {}", userContext);
        String sysPrompt = "You are an expert productivity coach. Provide 3 short, actionable focus tips based on the user's context. Keep the response concise.";
        return callOpenAi(sysPrompt, userContext);
    }

    public String summarizeResource(String resourceContent) {
        logger.info("Summarizing resource...");
        String sysPrompt = "You are an AI assistant. Extract the key concepts and provide a concise summary of the provided text.";
        return callOpenAi(sysPrompt, resourceContent);
    }

    public String generateDailyPlan(String userContext) {
        logger.info("Generating daily plan for context: {}", userContext);
        String sysPrompt = "You are a daily planning assistant. Create a brief, structured daily plan (Morning, Afternoon, Evening) based on the user's context.";
        return callOpenAi(sysPrompt, userContext);
    }

    public String generateWeeklyReview(String userContext) {
        logger.info("Generating weekly review for context: {}", userContext);
        String sysPrompt = "You are a performance analyst. Write a short, encouraging weekly review summarizing the user's achievements and areas for improvement based on the context.";
        return callOpenAi(sysPrompt, userContext);
    }

    public String generateSuggestedLinks(String topicContext) {
        logger.info("Generating suggested links for context: {}", topicContext);
        String sysPrompt = "Provide exactly 3 real, educational URL links related to the user's context. Format your response strictly as a numbered list with a brief description. E.g., '1. https://example.com - Description'";
        return callOpenAi(sysPrompt, topicContext);
    }

    public String generateTrendingResources(String topicContext) {
        logger.info("Generating trending resources for context: {}", topicContext);
        String sysPrompt = "Suggest 3 trending or highly recommended resources (books, PDFs, videos) related to the user's context. Format strictly as a numbered list. E.g., '1. The Title.pdf - Author'";
        return callOpenAi(sysPrompt, topicContext);
    }
}
