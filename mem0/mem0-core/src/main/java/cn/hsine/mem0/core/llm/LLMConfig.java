package cn.hsine.mem0.core.llm;

import java.util.Collections;
import java.util.Map;

/**
 * Configuration for an LLM generation request.
 *
 * @param temperature the sampling temperature (0.0 - 2.0)
 * @param maxTokens the maximum number of tokens to generate
 * @param topP the nucleus sampling parameter
 * @param additionalParams provider-specific additional parameters
 * @author MoBai

 */
public record LLMConfig(Double temperature, Integer maxTokens, Double topP, Map<String, Object> additionalParams) {

    /**
     * Creates a config with default values.
     */
    public LLMConfig() {
        this(0.2, 1500, null, null);
    }

    /**
     * Creates a config with temperature and max tokens.
     *
     * @param temperature the sampling temperature
     * @param maxTokens the maximum tokens
     */
    public LLMConfig(Double temperature, Integer maxTokens) {
        this(temperature, maxTokens, null, null);
    }

    /**
     * Canonical constructor with null-safe additionalParams.
     */
    public LLMConfig {
        additionalParams = additionalParams != null
            ? Collections.unmodifiableMap(additionalParams)
            : Collections.emptyMap();
    }

    /**
     * Gets an additional parameter value.
     *
     * @param key the key
     * @return the value, or null if not present
     */
    public Object getAdditionalParam(String key) {
        return additionalParams.get(key);
    }

    /**
     * Gets an additional parameter value with a default.
     *
     * @param key the key
     * @param defaultValue the default value
     * @param <T> the type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalParam(String key, T defaultValue) {
        Object value = additionalParams.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
