package com.mindforge.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration loader for Google OAuth settings.
 * Reads from config.properties in the classpath.
 */
public class GoogleOAuthConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();
    private static GoogleOAuthConfig instance;

    private GoogleOAuthConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("WARNING: " + CONFIG_FILE + " not found, using defaults");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + CONFIG_FILE, e);
        }
    }

    public static synchronized GoogleOAuthConfig getInstance() {
        if (instance == null) {
            instance = new GoogleOAuthConfig();
        }
        return instance;
    }

    // Google OAuth Credentials
    public String getClientId() {
        return props.getProperty("google.client.id", "");
    }

    public String getClientSecret() {
        return props.getProperty("google.client.secret", "");
    }

    // OAuth Endpoints
    public String getAuthUrl() {
        return props.getProperty("google.oauth.auth.url",
                "https://accounts.google.com/o/oauth2/v2/auth");
    }

    public String getTokenUrl() {
        return props.getProperty("google.oauth.token.url",
                "https://oauth2.googleapis.com/token");
    }

    public String getUserInfoUrl() {
        return props.getProperty("google.oauth.userinfo.url",
                "https://www.googleapis.com/oauth2/v2/userinfo");
    }

    public String getRevokeUrl() {
        return props.getProperty("google.oauth.revoke.url",
                "https://oauth2.googleapis.com/revoke");
    }

    // Redirect & Server
    public String getRedirectUri() {
        return props.getProperty("google.redirect.uri",
                "http://localhost:8888/Callback");
    }

    public int getLocalPort() {
        try {
            return Integer.parseInt(props.getProperty("oauth.local.port", "8888"));
        } catch (NumberFormatException e) {
            return 8888;
        }
    }

    public String getLocalPath() {
        return props.getProperty("oauth.local.path", "/Callback");
    }

    public int getTimeoutSeconds() {
        try {
            return Integer.parseInt(props.getProperty("oauth.timeout.seconds", "120"));
        } catch (NumberFormatException e) {
            return 120;
        }
    }

    // Scopes
    public String getScopes() {
        return props.getProperty("google.scopes", "openid email profile");
    }

    public String getScopesEncoded() {
        return props.getProperty("google.scopes.encoded", "openid%20email%20profile");
    }

    // API Backend
    public String getApiBaseUrl() {
        return props.getProperty("api.base.url", "http://localhost:8000");
    }

    public String getApiGoogleAuthUrl() {
        return getApiBaseUrl() + props.getProperty("api.auth.google", "/api/auth/google");
    }

    public String getApiCallbackUrl() {
        return getApiBaseUrl() + props.getProperty("api.auth.callback", "/api/auth/google/callback");
    }

    public String getApiVerifyUrl() {
        return getApiBaseUrl() + props.getProperty("api.auth.verify", "/api/auth/verify-token");
    }

    // Timeouts
    public int getConnectTimeoutMs() {
        try {
            return Integer.parseInt(props.getProperty("api.timeout.connect", "10000"));
        } catch (NumberFormatException e) {
            return 10000;
        }
    }

    public int getReadTimeoutMs() {
        try {
            return Integer.parseInt(props.getProperty("api.timeout.read", "30000"));
        } catch (NumberFormatException e) {
            return 30000;
        }
    }
}