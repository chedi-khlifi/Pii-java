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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fetches learning resource suggestions from the Open Library API.
 * Mirrors: ExternalLearningResourceService.php → fetchOpenLibrarySuggestions()
 *
 * Endpoint: https://openlibrary.org/search.json
 * Used in: guardian-library.fxml → "External Suggestions" section
 */
public class ExternalLearningResourceService {

    private static final String OPEN_LIBRARY_ENDPOINT = "https://openlibrary.org/search.json";
    private static final int TIMEOUT_SECONDS = 6;

    private final HttpClient httpClient;

    public ExternalLearningResourceService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Fetches book suggestions from Open Library for a given query.
     *
     * @param query search term (defaults to "study skills" if blank)
     * @param limit max results 1–10
     * @param page  page number (1-based)
     * @return list of maps with keys: title, author, year, url
     */
    public List<Map<String, Object>> fetchOpenLibrarySuggestions(String query, int limit, int page) {
        String safeQuery = (query == null || query.isBlank()) ? "study skills" : query.trim();
        int safeLimit = Math.max(1, Math.min(10, limit));
        int safePage  = Math.max(1, page);

        try {
            String url = OPEN_LIBRARY_ENDPOINT
                    + "?q=" + URLEncoder.encode(safeQuery, StandardCharsets.UTF_8)
                    + "&limit=" + safeLimit
                    + "&page=" + safePage
                    + "&language=eng";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return Collections.emptyList();
            }

            JSONObject payload = new JSONObject(response.body());
            JSONArray docs = payload.optJSONArray("docs");
            if (docs == null) return Collections.emptyList();

            List<Map<String, Object>> results = new ArrayList<>();

            for (int i = 0; i < docs.length() && results.size() < safeLimit; i++) {
                JSONObject doc = docs.optJSONObject(i);
                if (doc == null) continue;

                String title = doc.optString("title", "").trim();
                if (title.isEmpty()) continue;

                // Author
                String author = "Unknown author";
                JSONArray authorNames = doc.optJSONArray("author_name");
                if (authorNames != null && authorNames.length() > 0) {
                    author = authorNames.optString(0, author);
                }

                // Year
                Integer year = null;
                if (doc.has("first_publish_year")) {
                    year = doc.optInt("first_publish_year");
                }

                // URL
                String workKey = doc.optString("key", "").trim();
                String bookUrl = workKey.isEmpty()
                        ? "https://openlibrary.org"
                        : "https://openlibrary.org" + workKey;

                results.add(Map.of(
                        "title",  title,
                        "author", author,
                        "year",   year != null ? year : "",
                        "url",    bookUrl
                ));
            }

            return results;

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Convenience overload — uses default limit=6, page=1. */
    public List<Map<String, Object>> fetchOpenLibrarySuggestions(String query) {
        return fetchOpenLibrarySuggestions(query, 6, 1);
    }
}
