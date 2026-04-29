package com.mindforge.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration loader for Groq AI API settings.
 * Reads from config.properties in the classpath.
 */
public class GroqConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();
    private static GroqConfig instance;

    private GroqConfig() {
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

    public static synchronized GroqConfig getInstance() {
        if (instance == null) {
            instance = new GroqConfig();
        }
        return instance;
    }

    public String getApiKey() {
        return props.getProperty("groq.api.key", "");
    }

    public String getApiUrl() {
        return props.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    }

    public String getModel() {
        return props.getProperty("groq.model", "llama-3.1-8b-instant");
    }
}
