package com.mem0.core.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for LLM provider selection and connection settings.
 *
 * @author MoBai

 */
@Component
@ConfigurationProperties(prefix = "mem0.llm")
public class LLMProviderConfig {

    /**
     * The LLM provider (e.g., "openai", "anthropic", "azure", "gemini", "ollama").
     */
    private String provider = "openai";

    /**
     * Provider-specific configuration (api-key, model, base-url, etc.).
     */
    private Map<String, Object> config = new HashMap<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    /**
     * Gets a configuration value.
     *
     * @param key the key
     * @return the value
     */
    public Object get(String key) {
        return config.get(key);
    }

    /**
     * Gets a configuration value with a default.
     *
     * @param key the key
     * @param defaultValue the default value
     * @param <T> the type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = config.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
