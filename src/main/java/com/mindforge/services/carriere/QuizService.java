package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.OpportuniteCarriere;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizService {

    private static String getEnvOrFile(String key) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) return val;
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    if (line.trim().startsWith(key + "=")) {
                        return line.substring(line.indexOf('=') + 1).trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static final String API_KEY = getEnvOrFile("RJAB_OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    // In-memory session tracking (per app run, per opportunity)
    private static final Map<Integer, Integer> attemptsMap = new HashMap<>();
    private static final Map<Integer, Boolean>  passedMap  = new HashMap<>();

    public static final int MAX_ATTEMPTS  = 3;
    public static final int PASSING_SCORE = 4;
    public static final int TOTAL_QUESTIONS = 5;

    // ── Question model ────────────────────────────────────────────────────────

    public record QuizQuestion(String question, List<String> options, int correct) {}

    // ── Attempt tracking ──────────────────────────────────────────────────────

    public int getAttempts(int oppId) {
        return attemptsMap.getOrDefault(oppId, 0);
    }

    public boolean isPassed(int oppId) {
        return Boolean.TRUE.equals(passedMap.get(oppId));
    }

    public void incrementAttempts(int oppId) {
        attemptsMap.put(oppId, getAttempts(oppId) + 1);
    }

    public void markPassed(int oppId) {
        passedMap.put(oppId, true);
    }

    // ── OpenAI question generation ────────────────────────────────────────────

    public List<QuizQuestion> generateQuestions(OpportuniteCarriere opp) throws Exception {
        String description = opp.getDescription() != null
                ? opp.getDescription().substring(0, Math.min(opp.getDescription().length(), 1500))
                : "";

        String prompt = String.format(
            "You are a professional recruiter. Generate exactly 5 multiple-choice quiz questions " +
            "to test a candidate's fit and knowledge for the following job posting.\n\n" +
            "Job Title: %s\nJob Type: %s\nDescription: %s\n\n" +
            "Return ONLY a valid JSON array (no markdown, no explanation) with exactly 5 objects, each having:\n" +
            "- \"question\": the question text\n" +
            "- \"options\": an array of exactly 4 strings, each prefixed with \"A. \", \"B. \", \"C. \", \"D. \"\n" +
            "- \"correct\": the 0-based index of the correct option\n\n" +
            "Example: [{\"question\": \"...\", \"options\": [\"A. ...\", \"B. ...\", \"C. ...\", \"D. ...\"], \"correct\": 2}]",
            opp.getTitle(),
            opp.getType() != null ? opp.getType() : "General",
            description
        );

        // Build request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("temperature", 0.7);
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);
        requestBody.put("messages", messages);

        // HTTP call
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " — " + response.body());
        }

        // Parse response
        JSONObject responseJson = new JSONObject(response.body());
        String content = responseJson
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

        // Strip markdown code fences if present
        if (content.startsWith("```")) {
            content = content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }

        JSONArray questionsJson = new JSONArray(content);
        if (questionsJson.length() != TOTAL_QUESTIONS) {
            throw new RuntimeException("OpenAI returned " + questionsJson.length() + " questions instead of 5.");
        }

        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 0; i < questionsJson.length(); i++) {
            JSONObject q = questionsJson.getJSONObject(i);
            JSONArray opts = q.getJSONArray("options");
            List<String> options = new ArrayList<>();
            for (int j = 0; j < opts.length(); j++) {
                options.add(opts.getString(j));
            }
            questions.add(new QuizQuestion(
                q.getString("question"),
                options,
                q.getInt("correct")
            ));
        }
        return questions;
    }
}
