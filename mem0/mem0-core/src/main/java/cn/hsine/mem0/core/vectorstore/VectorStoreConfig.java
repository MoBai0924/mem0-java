package cn.hsine.mem0.core.vectorstore;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for vector store.
 *
 * @author MoBai

 */
@Component
@ConfigurationProperties(prefix = "mem0.vector-store")
public class VectorStoreConfig {

    /**
     * The vector store provider (e.g., "pgvector", "qdrant", "chroma").
     */
    private String provider = "pgvector";

    /**
     * Provider-specific configuration.
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
