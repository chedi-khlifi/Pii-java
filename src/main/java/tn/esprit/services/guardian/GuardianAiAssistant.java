package tn.esprit.services.guardian;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-provider AI assistant for the Guardian module.
 * Mirrors: GuardianAiAssistant.php — supports OpenAI, Gemini, Claude with rule-based fallbacks.
 *
 * Methods ported:
 *   recommendDuration()        — Pomodoro duration recommendation
 *   getFocusTips()             — 3 focus tips + motivation quote
 *   buildDailyPlan()           — 3-item daily plan
 *   buildWeeklyReview()        — weekly summary + wins + next action
 *   generateLearningResource() — full study resource (markdown/PDF content)
 *   validateLearningResourceRequest() — content safety check
 *   getAllowedResourceTypes()  — pdf, summary, cheat_sheet, exercise
 *   isExternalAiAvailable()    — checks if an API key is configured
 */
public class GuardianAiAssistant {

    public static final List<String> ALLOWED_RESOURCE_TYPES =
            List.of("pdf", "summary", "cheat_sheet", "exercise");

    private static final String OPENAI_ENDPOINT  = "https://api.openai.com/v1/chat/completions";
    private static final String GEMINI_TEMPLATE  = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final String CLAUDE_ENDPOINT  = "https://api.anthropic.com/v1/messages";
    private static final String QUOTE_ENDPOINT   = "https://zenquotes.io/api/random";

    private final HttpClient httpClient;
    private final String provider;
    private final String openAiApiKey;
    private final String openAiModel;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String claudeApiKey;
    private final String claudeModel;

    public GuardianAiAssistant() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();

        // Read provider + keys from env / .env file (mirrors Symfony DI config)
        this.provider     = getEnv("GUARDIAN_AI_PROVIDER", "openai");
        this.openAiApiKey = getEnv("OPENAI_API_KEY", "");
        this.openAiModel  = getEnv("GUARDIAN_OPENAI_MODEL", "gpt-4o-mini");
        this.geminiApiKey = getEnv("GEMINI_API_KEY", "");
        this.geminiModel  = getEnv("GUARDIAN_GEMINI_MODEL", "gemini-1.5-flash");
        this.claudeApiKey = getEnv("CLAUDE_API_KEY", "");
        this.claudeModel  = getEnv("GUARDIAN_CLAUDE_MODEL", "claude-3-5-haiku-latest");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Recommends a Pomodoro duration for a task.
     * Returns map with: duration (int), reason (String), source ("ai"|"rule")
     */
    public Map<String, Object> recommendDuration(Map<String, Object> taskContext, Map<String, Object> stats) {
        int priority = toInt(taskContext.get("priority"), 2);
        int ruleBased = ruleBasedDuration(priority);

        if (!isAiConfigured()) {
            return Map.of("duration", ruleBased,
                          "reason",   "Rule-based fallback (AI key not configured).",
                          "source",   "rule");
        }

        String system = "You are a productivity assistant for a Pomodoro app. Return strictly valid JSON with fields: duration (int), reason (string). No markdown.";
        String user   = "Recommend a focus duration in minutes based on this context: "
                + toJson(Map.of("task", taskContext, "user_stats", stats,
                                "constraints", Map.of("min_duration", 15, "max_duration", 90, "step", 5)));

        JSONObject data = callStructuredAi(system, user, 220);
        if (data == null || !data.has("duration")) {
            return Map.of("duration", ruleBased,
                          "reason",   "Rule-based fallback (AI parse error).",
                          "source",   "rule");
        }

        int raw     = data.optInt("duration", ruleBased);
        int bounded = Math.max(15, Math.min(90, raw));
        int rounded = (int) (Math.round(bounded / 5.0) * 5);

        return Map.of("duration", rounded,
                      "reason",   data.optString("reason", "AI recommendation."),
                      "source",   "ai");
    }

    /**
     * Returns 3 focus tips + a motivation quote.
     * Returns map with: tips (List<String>), motivation (String), source
     */
    public Map<String, Object> getFocusTips(Map<String, Object> taskContext, Map<String, Object> stats) {
        List<String> fallbackTips = List.of(
                "Start with a single clear outcome for this session.",
                "Mute distractions and keep only one tab/app open.",
                "Take a 5-minute break after each completed session."
        );
        String quote = fetchMotivationQuote();

        if (!isAiConfigured()) {
            return Map.of("tips", fallbackTips, "motivation", quote, "source", "rule");
        }

        String system = "You are a concise study coach. Return strictly valid JSON with keys: tips (array of 3 short strings), motivation (string). No markdown.";
        String user   = "Generate focus tips for this context: "
                + toJson(Map.of("task", taskContext, "stats", stats));

        JSONObject data = callStructuredAi(system, user, 260);
        if (data == null || !data.has("tips")) {
            return Map.of("tips", fallbackTips, "motivation", quote, "source", "rule");
        }

        List<String> tips = jsonArrayToStringList(data.optJSONArray("tips"), 3, fallbackTips);
        return Map.of("tips", tips,
                      "motivation", data.optString("motivation", quote),
                      "source", "ai");
    }

    /**
     * Builds a 3-item daily focus plan.
     * Returns map with: plan (List<String>), source
     */
    public Map<String, Object> buildDailyPlan(List<Map<String, Object>> tasks, Map<String, Object> stats) {
        List<String> defaultPlan = List.of(
                "Start with one high-priority task block.",
                "Run two medium tasks in focused sessions.",
                "Finish with a short review and tomorrow prep."
        );

        if (!isAiConfigured()) {
            return Map.of("plan", defaultPlan, "source", "rule");
        }

        List<Map<String, Object>> sliced = tasks.size() > 10 ? tasks.subList(0, 10) : tasks;
        String system = "You are a planning assistant. Return strictly valid JSON with key plan (array of 3 short actionable items). No markdown.";
        String user   = "Create a practical daily focus plan: " + toJson(Map.of("tasks", sliced, "stats", stats));

        JSONObject data = callStructuredAi(system, user, 260);
        if (data == null || !data.has("plan")) {
            return Map.of("plan", defaultPlan, "source", "rule");
        }

        List<String> plan = jsonArrayToStringList(data.optJSONArray("plan"), 3, defaultPlan);
        return Map.of("plan", plan, "source", "ai");
    }

    /**
     * Builds a weekly review with summary, wins, and next action.
     * Returns map with: summary, wins (List<String>), next_action, source
     */
    public Map<String, Object> buildWeeklyReview(Map<String, Object> stats,
                                                  List<Map<String, Object>> recentSessions) {
        List<String> fallbackWins = List.of(
                "Keep your top task streak active.",
                "Protect one distraction-free block daily."
        );
        Map<String, Object> fallback = Map.of(
                "summary",     "You are building consistency with regular focus blocks this week.",
                "wins",        fallbackWins,
                "next_action", "Schedule one 35-minute session on your highest-priority task today.",
                "source",      "rule"
        );

        if (!isAiConfigured()) return fallback;

        List<Map<String, Object>> sliced = recentSessions.size() > 8
                ? recentSessions.subList(0, 8) : recentSessions;
        String system = "You are a concise productivity coach. Return strictly valid JSON with keys: summary (string), wins (array of 2 short strings), next_action (string). No markdown.";
        String user   = "Create a weekly focus review from this context: "
                + toJson(Map.of("stats", stats, "recent_sessions", sliced));

        JSONObject data = callStructuredAi(system, user, 260);
        if (data == null || !data.has("summary") || !data.has("next_action")) return fallback;

        List<String> wins = jsonArrayToStringList(data.optJSONArray("wins"), 2, fallbackWins);
        String summary    = data.optString("summary", "").trim();
        String nextAction = data.optString("next_action", "").trim();

        return Map.of(
                "summary",     summary.isEmpty()    ? fallback.get("summary")     : summary,
                "wins",        wins,
                "next_action", nextAction.isEmpty() ? fallback.get("next_action") : nextAction,
                "source",      "ai"
        );
    }

    /**
     * Generates a full learning resource (markdown content).
     * Returns map with: title, content, source
     */
    public Map<String, Object> generateLearningResource(Map<String, Object> context) {
        String subject       = str(context.get("subject"), "General");
        String description   = str(context.get("description"), "");
        String studentDemand = str(context.get("student_demand"), "");
        String resourceType  = str(context.get("resource_type"), "summary");
        String titleHint     = str(context.get("title_hint"), "");

        String fallbackTitle   = titleHint.isEmpty() ? subject + " - Study Resource" : titleHint;
        String fallbackContent = buildLocalResourceContent(subject, description, studentDemand, resourceType, fallbackTitle);

        if (!isAiConfigured()) {
            return Map.of("title", fallbackTitle, "content", fallbackContent, "source", "local");
        }

        String system = "You are an educational content assistant. Return strictly valid JSON with keys: title (string), content (string markdown). No markdown outside JSON.";
        String user   = "Generate a complete learning resource from this context: "
                + toJson(Map.of(
                        "subject", subject, "description", description,
                        "student_demand", studentDemand, "resource_type", resourceType,
                        "title_hint", titleHint,
                        "constraints", Map.of(
                                "format", "markdown",
                                "content_length_target", "350-700 words",
                                "include_sections", List.of("Learning objective", "Core explanation",
                                        "Practical steps", "Self-check questions"))));

        JSONObject data = callStructuredAi(system, user, 1100);
        if (data == null || !data.has("title") || !data.has("content")) {
            return Map.of("title", fallbackTitle, "content", fallbackContent, "source", "local");
        }

        String title   = data.optString("title", "").trim();
        String content = data.optString("content", "").trim();
        if (title.isEmpty())   title   = fallbackTitle;
        if (content.isEmpty()) content = fallbackContent;

        // Truncate to safe limits (mirrors PHP mb_substr)
        if (title.length()   > 255)   title   = title.substring(0, 255);
        if (content.length() > 12000) content = content.substring(0, 12000);

        return Map.of("title", title, "content", content, "source", "ai");
    }

    /**
     * Validates that a resource generation request is study-related and not blocked.
     * Returns map with: valid (boolean), message (String|null)
     */
    public Map<String, Object> validateLearningResourceRequest(String subject, String description,
                                                                String studentDemand, String resourceType) {
        if (!ALLOWED_RESOURCE_TYPES.contains(resourceType)) {
            return Map.of("valid", false, "message", "Requested file type is not allowed for AI generation.");
        }

        String combined = (subject + " " + description + " " + studentDemand).toLowerCase();

        List<String> blockedTopics = List.of(
                "football", "soccer", "fifa", "nba", "nfl", "tennis", "boxing", "ufc",
                "transfer market", "premier league", "champions league"
        );
        for (String topic : blockedTopics) {
            if (combined.contains(topic)) {
                return Map.of("valid", false,
                        "message", "Only study-related educational content is allowed. Sports topics are blocked.");
            }
        }

        List<String> studyAllowlist = List.of(
                "study", "learning", "learn", "education", "course", "lesson", "revision",
                "exercise", "exam", "quiz", "homework", "assignment", "school", "university",
                "student", "teacher", "class", "math", "mathematics", "algebra", "geometry",
                "physics", "chemistry", "biology", "history", "geography", "literature",
                "programming", "informatique", "étude", "etud", "apprentissage", "éducation",
                "cours", "leçon", "lecon", "révision", "exercice", "devoir", "contrôle",
                "controle", "bac", "licence", "master", "matière", "matiere"
        );
        boolean hasStudySignal = studyAllowlist.stream().anyMatch(combined::contains);
        if (!hasStudySignal) {
            return Map.of("valid", false,
                    "message", "Only study-related educational content is allowed. Please provide an academic learning request.");
        }

        return Map.of("valid", true, "message", "");
    }

    public List<String> getAllowedResourceTypes() { return ALLOWED_RESOURCE_TYPES; }

    public boolean isExternalAiAvailable() { return isAiConfigured(); }

    // ── AI provider dispatch ──────────────────────────────────────────────────

    private JSONObject callStructuredAi(String system, String user, int maxTokens) {
        return switch (getProvider()) {
            case "gemini" -> callGemini(system, user, maxTokens);
            case "claude" -> callClaude(system, user, maxTokens);
            default       -> callOpenAi(system, user, maxTokens);
        };
    }

    private JSONObject callOpenAi(String system, String user, int maxTokens) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", openAiModel);
            body.put("temperature", 0.3);
            body.put("max_tokens", maxTokens);
            body.put("response_format", new JSONObject().put("type", "json_object"));
            body.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", system))
                    .put(new JSONObject().put("role", "user").put("content", user)));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_ENDPOINT))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return null;

            JSONObject payload = new JSONObject(resp.body());
            String raw = payload.optJSONArray("choices")
                    .optJSONObject(0).optJSONObject("message").optString("content", "");
            return decodeJsonText(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject callGemini(String system, String user, int maxTokens) {
        try {
            String endpoint = String.format(GEMINI_TEMPLATE, geminiModel) + "?key=" + geminiApiKey;

            JSONObject body = new JSONObject();
            body.put("generationConfig", new JSONObject()
                    .put("temperature", 0.3)
                    .put("maxOutputTokens", maxTokens)
                    .put("responseMimeType", "application/json"));
            body.put("contents", new JSONArray().put(new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject()
                            .put("text", system + "\n\n" + user)))));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return null;

            JSONObject payload = new JSONObject(resp.body());
            String raw = payload.optJSONArray("candidates")
                    .optJSONObject(0).optJSONObject("content")
                    .optJSONArray("parts").optJSONObject(0).optString("text", "");
            return decodeJsonText(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject callClaude(String system, String user, int maxTokens) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", claudeModel);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.3);
            body.put("system", system);
            body.put("messages", new JSONArray().put(new JSONObject()
                    .put("role", "user").put("content", user)));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(CLAUDE_ENDPOINT))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", claudeApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return null;

            JSONObject payload = new JSONObject(resp.body());
            String raw = payload.optJSONArray("content")
                    .optJSONObject(0).optString("text", "");
            return decodeJsonText(raw);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Motivation quote ──────────────────────────────────────────────────────

    private String fetchMotivationQuote() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(QUOTE_ENDPOINT))
                    .timeout(Duration.ofSeconds(4))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return defaultQuote();
            JSONArray arr = new JSONArray(resp.body());
            if (arr.isEmpty()) return defaultQuote();
            JSONObject q = arr.optJSONObject(0);
            String quote  = q == null ? "" : q.optString("q", "").trim();
            String author = q == null ? "" : q.optString("a", "Unknown").trim();
            return quote.isEmpty() ? defaultQuote() : quote + " — " + author;
        } catch (Exception e) {
            return defaultQuote();
        }
    }

    private static String defaultQuote() {
        return "Small progress every day builds strong momentum.";
    }

    // ── Local resource content generator ─────────────────────────────────────

    private String buildLocalResourceContent(String subject, String description,
                                              String studentDemand, String resourceType, String title) {
        String need = studentDemand.isBlank()
                ? "Understand the core concepts and practice with exercises." : studentDemand;
        String desc = description.isBlank()
                ? "Structured explanation with practical examples." : description;

        String subjectLc = subject.toLowerCase();
        String needLc    = (need + " " + desc).toLowerCase();

        // Special case: linear algebra / vector spaces (mirrors PHP buildVectorSpaceExercisePack)
        if (subjectLc.contains("math") && (
                needLc.contains("espace vectoriel") || needLc.contains("vector space") ||
                needLc.contains("algèbre linéaire") || needLc.contains("algebre lineaire"))) {
            return buildVectorSpaceExercisePack(title, resourceType);
        }

        return "# " + title + "\n\n"
                + "## Learning Objective\n"
                + "This " + resourceType + " resource focuses on **" + subject
                + "** and targets the student need: **" + need + "**.\n\n"
                + "## Context\n" + desc + "\n\n"
                + "## Core Explanation\n"
                + "- Identify key definitions and notation for the topic.\n"
                + "- Break the topic into 3 short concept blocks.\n"
                + "- For each block, write one simple example and one common mistake.\n\n"
                + "## Practical Steps\n"
                + "1. Review definitions for 10 minutes.\n"
                + "2. Solve 2 guided examples step by step.\n"
                + "3. Complete 3 independent exercises with correction.\n"
                + "4. Summarize what to remember in 5 bullet points.\n\n"
                + "## Practice Exercises\n"
                + "1. Basic exercise: apply the main definition in a direct case.\n"
                + "2. Medium exercise: combine two concepts in one problem.\n"
                + "3. Challenge exercise: justify the method and verify the result.\n\n"
                + "## Self-check Questions\n"
                + "- Can I explain the concept in my own words?\n"
                + "- Can I solve a similar exercise without help?\n"
                + "- Which step is still unclear and needs revision?\n\n"
                + "## 30-Minute Study Plan\n"
                + "- 10 min: concept recap\n"
                + "- 15 min: exercise solving\n"
                + "- 5 min: correction + notes\n";
    }

    private String buildVectorSpaceExercisePack(String title, String resourceType) {
        String focus = "exercise".equals(resourceType) ? "Pack orienté exercices" : "Fiche + exercices guidés";
        return "# " + title + "\n\n"
                + "## Objectif\n"
                + focus + " sur les **espaces vectoriels** (niveau licence).\n\n"
                + "## Rappels essentiels\n"
                + "- Un sous-ensemble F est un sous-espace si: (i) 0∈F, (ii) u,v∈F ⇒ u+v∈F, (iii) λ∈ℝ, u∈F ⇒ λu∈F.\n"
                + "- Une famille est **libre** si la combinaison linéaire nulle est triviale.\n"
                + "- Une base est une famille libre et génératrice.\n\n"
                + "## Exercice 1 — Sous-espace ?\n"
                + "Dans ℝ³, étudier F={(x,y,z) | x-2y+z=0}.\n"
                + "**Correction :** Oui, c'est le noyau d'une forme linéaire.\n\n"
                + "## Exercice 2 — Contre-exemple\n"
                + "Dans ℝ², étudier G={(x,y) | x+y=1}.\n"
                + "**Correction :** Non, 0∉G car 0+0≠1.\n\n"
                + "## Exercice 3 — Liberté linéaire\n"
                + "Étudier la liberté de u₁=(1,0,1), u₂=(2,1,3), u₃=(0,1,1) dans ℝ³.\n"
                + "**Correction :** Famille libre (système homogène → solution triviale).\n\n"
                + "## Plan de travail (45 min)\n"
                + "- 10 min: rappels + définitions.\n"
                + "- 25 min: exercices 1 à 3.\n"
                + "- 10 min: correction + fiche erreurs fréquentes.\n";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAiConfigured() {
        return switch (getProvider()) {
            case "gemini" -> !geminiApiKey.isBlank();
            case "claude" -> !claudeApiKey.isBlank();
            default       -> !openAiApiKey.isBlank();
        };
    }

    private String getProvider() {
        String p = provider.toLowerCase().trim();
        return List.of("openai", "gemini", "claude").contains(p) ? p : "openai";
    }

    /** Mirrors ruleBasedDuration(): priority 3→50, 2→35, default→25 */
    public int ruleBasedDuration(int priority) {
        return switch (priority) {
            case 3 -> 50;
            case 2 -> 35;
            default -> 25;
        };
    }

    private JSONObject decodeJsonText(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String text = raw.trim();
        try {
            return new JSONObject(text);
        } catch (Exception ignored) {}
        // Try to extract JSON object from surrounding text
        Matcher m = Pattern.compile("\\{.*}", Pattern.DOTALL).matcher(text);
        if (m.find()) {
            try { return new JSONObject(m.group()); } catch (Exception ignored) {}
        }
        return null;
    }

    private List<String> jsonArrayToStringList(JSONArray arr, int maxItems, List<String> fallback) {
        if (arr == null) return fallback;
        List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length() && result.size() < maxItems; i++) {
            String s = arr.optString(i, "").trim();
            if (!s.isEmpty()) result.add(s);
        }
        return result.size() < maxItems ? fallback : result;
    }

    private static String toJson(Object obj) {
        try {
            if (obj instanceof Map<?, ?> m) return new JSONObject(m).toString();
            if (obj instanceof List<?> l)  return new JSONArray(l).toString();
            return String.valueOf(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String str(Object val, String def) {
        if (val == null) return def;
        String s = val.toString().trim();
        return s.isEmpty() ? def : s;
    }

    private static int toInt(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val.toString().trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private static String getEnv(String key, String defaultValue) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val.trim();
        // Fall back to .env file
        try {
            java.nio.file.Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath)) {
                    line = line.trim();
                    if (line.startsWith(key + "=")) {
                        return line.substring(key.length() + 1).trim().replace("\"", "");
                    }
                }
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }
}
