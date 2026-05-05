package tn.esprit.services.guardian;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ExternalLearningResourceService {

    private static final String OPEN_LIBRARY_SEARCH_ENDPOINT = "https://openlibrary.org/search.json";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public List<ExternalSuggestion> fetchOpenLibrarySuggestions(String query, int limit, int page) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isEmpty()) {
            safeQuery = "study skills";
        }

        int safeLimit = Math.max(1, Math.min(10, limit));
        int safePage = Math.max(1, page);

        try {
            String url = OPEN_LIBRARY_SEARCH_ENDPOINT
                    + "?q=" + URLEncoder.encode(safeQuery, StandardCharsets.UTF_8)
                    + "&limit=" + safeLimit
                    + "&page=" + safePage
                    + "&language=eng";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return List.of();
            }

            JSONObject payload = new JSONObject(response.body());
            JSONArray docs = payload.optJSONArray("docs");
            if (docs == null) {
                return List.of();
            }

            List<ExternalSuggestion> results = new ArrayList<>();
            for (int i = 0; i < docs.length() && results.size() < safeLimit; i++) {
                JSONObject doc = docs.optJSONObject(i);
                if (doc == null) {
                    continue;
                }

                String title = doc.optString("title", "").trim();
                if (title.isEmpty()) {
                    continue;
                }

                String author = "Unknown author";
                JSONArray authors = doc.optJSONArray("author_name");
                if (authors != null && authors.length() > 0) {
                    author = authors.optString(0, author);
                }

                Integer year = null;
                if (doc.has("first_publish_year")) {
                    year = doc.optInt("first_publish_year");
                }

                String workKey = doc.optString("key", "").trim();
                String url = workKey.isEmpty() ? "https://openlibrary.org" : "https://openlibrary.org" + workKey;

                results.add(new ExternalSuggestion(title, author, year, url));
            }

            return results;
        } catch (Exception e) {
            return List.of();
        }
    }
}
