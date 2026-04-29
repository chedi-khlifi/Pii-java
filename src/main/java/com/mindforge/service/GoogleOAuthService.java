package com.mindforge.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mindforge.config.GoogleOAuthConfig;
import javafx.application.Platform;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

/**
 * Service class handling Google OAuth 2.0 authentication flow.
 */
public class GoogleOAuthService {

    private final GoogleOAuthConfig config;
    private HttpServer server;
    private volatile String authCode;
    private volatile String authError;

    public GoogleOAuthService() {
        this.config = GoogleOAuthConfig.getInstance();
    }

    /**
     * Initiates Google OAuth flow and returns user info on success.
     */
    public CompletableFuture<GoogleUserInfo> authenticate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Reset state
                authCode = null;
                authError = null;

                // Start server first
                CountDownLatch latch = new CountDownLatch(1);
                startServer(latch);

                // Then open browser
                String authUrl = buildAuthorizationUrl();
                Platform.runLater(() -> openBrowser(authUrl));

                // Wait for callback (with timeout)
                boolean received = latch.await(config.getTimeoutSeconds(), TimeUnit.SECONDS);

                // Stop server
                stopServer();

                if (!received) {
                    throw new RuntimeException("Authorization timed out");
                }

                if (authError != null) {
                    throw new RuntimeException("OAuth error: " + authError);
                }

                if (authCode == null) {
                    throw new RuntimeException("No authorization code received");
                }

                System.out.println("Got auth code, exchanging for tokens...");

                // Exchange code for tokens
                TokenResponse tokens = exchangeCodeForTokens(authCode);

                System.out.println("Got tokens, fetching user info...");

                // Fetch user info
                return fetchUserInfo(tokens.accessToken);

            } catch (Exception e) {
                stopServer();
                throw new RuntimeException("OAuth authentication failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Starts HTTP server and waits for OAuth callback.
     */
    private void startServer(CountDownLatch latch) throws Exception {
        server = HttpServer.create(new InetSocketAddress(config.getLocalPort()), 0);

        server.createContext(config.getLocalPath(), (HttpExchange exchange) -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                System.out.println("Received callback: " + exchange.getRequestURI());

                Map<String, String> params = parseQueryString(query);
                authCode = params.get("code");
                authError = params.get("error");

                String responseHtml;
                int statusCode;

                if (authCode != null) {
                    responseHtml = "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset=\"UTF-8\"><title>Success</title>" +
                            "<style>" +
                            "body{font-family:Arial,sans-serif;text-align:center;padding-top:100px;background:#1a1a2e;color:white;}" +
                            ".success{color:#4CAF50;font-size:72px;margin-bottom:20px;}" +
                            "h2{margin-bottom:10px;}" +
                            "</style></head>" +
                            "<body>" +
                            "<div class=\"success\">&#10003;</div>" +
                            "<h2>Authentication Successful!</h2>" +
                            "<p>Return to MindForge app...</p>" +
                            "<script>setTimeout(function(){window.close()},2000);</script>" +
                            "</body></html>";
                    statusCode = 200;
                    System.out.println("Auth code received: " + authCode.substring(0, 20) + "...");
                } else {
                    String errorMsg = authError != null ? authError : "Unknown error";
                    responseHtml = "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset=\"UTF-8\"><title>Failed</title>" +
                            "<style>" +
                            "body{font-family:Arial,sans-serif;text-align:center;padding-top:100px;background:#1a1a2e;color:white;}" +
                            ".error{color:#f44336;font-size:72px;margin-bottom:20px;}" +
                            "</style></head>" +
                            "<body>" +
                            "<div class=\"error\">&#10007;</div>" +
                            "<h2>Authentication Failed</h2>" +
                            "<p>" + errorMsg + "</p>" +
                            "</body></html>";
                    statusCode = 400;
                    System.err.println("Auth error: " + errorMsg);
                }

                // Send response
                byte[] responseBytes = responseHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Release latch to signal main thread
                latch.countDown();
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("OAuth callback server started on port " + config.getLocalPort());
    }

    private void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("OAuth server stopped");
        }
    }

    /**
     * Parses query string into key-value map.
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                try {
                    map.put(pair[0], java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                } catch (Exception e) {
                    map.put(pair[0], pair[1]);
                }
            }
        }
        return map;
    }

    /**
     * Builds Google OAuth authorization URL.
     */
    private String buildAuthorizationUrl() {
        return String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline&prompt=consent",
                config.getAuthUrl(),
                URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8),
                config.getScopesEncoded()
        );
    }

    /**
     * Exchanges authorization code for access token.
     */
    private TokenResponse exchangeCodeForTokens(String code) throws Exception {
        URL url = URI.create(config.getTokenUrl()).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(config.getConnectTimeoutMs());
        conn.setReadTimeout(config.getReadTimeoutMs());

        String params = String.format(
                "code=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=authorization_code",
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getClientSecret(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8)
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String response;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            response = br.lines().collect(Collectors.joining());
        }

        if (responseCode != 200) {
            throw new RuntimeException("Token exchange failed: " + response);
        }

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return new TokenResponse(
                json.get("access_token").getAsString(),
                json.has("id_token") ? json.get("id_token").getAsString() : null,
                json.has("refresh_token") ? json.get("refresh_token").getAsString() : null,
                json.has("expires_in") ? json.get("expires_in").getAsInt() : 3600
        );
    }

    /**
     * Fetches user info from Google using access token.
     */
    private GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        URL url = URI.create(config.getUserInfoUrl()).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(config.getConnectTimeoutMs());
        conn.setReadTimeout(config.getReadTimeoutMs());

        int responseCode = conn.getResponseCode();
        String response;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            response = br.lines().collect(Collectors.joining());
        }

        if (responseCode != 200) {
            throw new RuntimeException("Failed to fetch user info: " + response);
        }

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return new GoogleUserInfo(
                json.get("id").getAsString(),
                json.get("email").getAsString(),
                json.has("name") ? json.get("name").getAsString() : "",
                json.has("picture") ? json.get("picture").getAsString() : "",
                json.has("verified_email") && json.get("verified_email").getAsBoolean(),
                accessToken
        );
    }

    /**
     * Opens browser to authorization URL.
     */
    private void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported() &&
                    java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(URI.create(url));
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            System.err.println("Please manually visit: " + url);
        }
    }

    // Data classes

    private static class TokenResponse {
        final String accessToken;
        final String idToken;
        final String refreshToken;
        final int expiresIn;

        TokenResponse(String accessToken, String idToken, String refreshToken, int expiresIn) {
            this.accessToken = accessToken;
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    public static class GoogleUserInfo {
        public final String googleId;
        public final String email;
        public final String name;
        public final String picture;
        public final boolean verifiedEmail;
        public final String accessToken;

        public GoogleUserInfo(String googleId, String email, String name,
                              String picture, boolean verifiedEmail, String accessToken) {
            this.googleId = googleId;
            this.email = email;
            this.name = name;
            this.picture = picture;
            this.verifiedEmail = verifiedEmail;
            this.accessToken = accessToken;
        }
    }
}