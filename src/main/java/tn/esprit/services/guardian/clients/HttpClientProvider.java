package tn.esprit.services.guardian.clients;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Provides a singleton HTTP client for all HTTP calls in the Guardian module.
 * Location: All REST API calls use this client for transport.
 */
public class HttpClientProvider {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static HttpClient getClient() {
        return client;
    }
}
