package com.example.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONObject;
import org.json.JSONArray;

public class OpenAIService {

    private static String getApiKey() {
        return ConfigLoader.get("openai.api.key");
    }

    private static boolean isGroqKey(String key) {
        return key != null && key.startsWith("gsk_");
    }

    private static String getChatUrl(String key) {
        return isGroqKey(key)
                ? "https://api.groq.com/openai/v1/chat/completions"
                : "https://api.openai.com/v1/chat/completions";
    }

    private static String getWhisperUrl(String key) {
        return isGroqKey(key)
                ? "https://api.groq.com/openai/v1/audio/transcriptions"
                : "https://api.openai.com/v1/audio/transcriptions";
    }

    private static String getChatModel(String key) {
        return isGroqKey(key) ? "llama-3.1-8b-instant" : "gpt-3.5-turbo";
    }

    /** Reads both input and error streams from a connection */
    private static String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        InputStream stream = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            return "{\"error\": \"HTTP " + status + " - no response body\"}";
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    public String generateChallenge(String topic, String category, String difficulty) throws Exception {
        String apiKey = getApiKey();

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_OPENAI_KEY")) {
            throw new IllegalStateException("Clé API manquante dans config.properties.");
        }

        String chatUrl   = getChatUrl(apiKey);
        String chatModel = getChatModel(apiKey);

        System.out.println("Using API: " + chatUrl);
        System.out.println("Model: " + chatModel);

        URL url = new URL(chatUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        String categoryInfo   = (category   != null && !category.isEmpty())   ? " de type " + category : "";
        String difficultyInfo = (difficulty != null && !difficulty.isEmpty()) ? " avec un niveau de difficulté " + difficulty : "";

        String prompt = "Générer un défi d'apprentissage court et créatif sur le thème : " + topic
                + categoryInfo + difficultyInfo
                + ". Tu DOIS impérativement utiliser le format suivant : TITRE | DESCRIPTION. "
                + "Exemple: Apprendre Java | Dans ce défi, vous allez créer une application... "
                + "Ne mets rien d'autre dans ta réponse, juste le titre suivi d'une barre verticale '|' puis la description.";

        // Escape the prompt for JSON
        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");

        String jsonBody = "{\"model\": \"" + chatModel + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + escapedPrompt + "\"}]}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        String responseStr = readResponse(conn);
        int status = conn.getResponseCode();
        System.out.println("HTTP Status: " + status);
        System.out.println("Raw Response: " + responseStr);

        if (status < 200 || status >= 300) {
            throw new RuntimeException("API error " + status + ": " + responseStr);
        }

        JSONObject jsonResponse = new JSONObject(responseStr);
        if (jsonResponse.has("choices")) {
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
                System.out.println("Extracted Content: " + content);
                return content;
            }
        }

        throw new RuntimeException("Unexpected response format: " + responseStr);
    }

    public String transcribeAudio(java.io.File audioFile) throws Exception {
        String apiKey = getApiKey();

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_OPENAI_KEY")) {
            return "Transcription désactivée (clé manquante).";
        }

        String whisperUrl = getWhisperUrl(apiKey);
        // Groq supports whisper-large-v3; OpenAI uses whisper-1
        String whisperModel = isGroqKey(apiKey) ? "whisper-large-v3" : "whisper-1";

        String boundary = "---boundary" + System.currentTimeMillis();
        URL url = new URL(whisperUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            // model field
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n".getBytes());
            os.write((whisperModel + "\r\n").getBytes());

            // file field
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"\r\n").getBytes());
            os.write("Content-Type: audio/wav\r\n\r\n".getBytes());
            Files.copy(audioFile.toPath(), os);
            os.write("\r\n".getBytes());

            os.write(("--" + boundary + "--\r\n").getBytes());
        }

        String responseStr = readResponse(conn);
        int status = conn.getResponseCode();
        System.out.println("Whisper HTTP Status: " + status);
        System.out.println("Whisper Response: " + responseStr);

        if (status < 200 || status >= 300) {
            throw new RuntimeException("Whisper API error " + status + ": " + responseStr);
        }

        JSONObject jsonResponse = new JSONObject(responseStr);
        if (jsonResponse.has("text")) {
            return jsonResponse.getString("text");
        }
        return responseStr;
    }
}
