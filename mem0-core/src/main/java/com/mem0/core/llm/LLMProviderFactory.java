package com.mem0.core.llm;

import com.mem0.core.exception.ConfigurationException;
import com.mem0.core.llm.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating LLM provider instances based on configuration.
 *
 * @author MoBai

 */
@Component
public class LLMProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LLMProviderFactory.class);

    public LLMProvider create(LLMProviderConfig config) {
        String provider = config.getProvider();
        log.debug("Creating LLM provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAILLM(config);
            case "anthropic" -> createAnthropicLLM(config);
            case "azure" -> createAzureOpenAILLM(config);
            case "gemini" -> createGeminiLLM(config);
            case "ollama" -> createOllamaLLM(config);
            case "deepseek" -> createDeepSeekLLM(config);
            case "groq" -> createGroqLLM(config);
            case "together" -> createTogetherLLM(config);
            case "vllm" -> createVLLMLLM(config);
            default -> throw new ConfigurationException("Unknown LLM provider: " + provider);
        };
    }

    private LLMProvider createOpenAILLM(LLMProviderConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("OpenAI API key is required");
        String baseUrl = config.get("base-url", "https://api.openai.com/v1");
        String model = config.get("model", "gpt-4o-mini");
        return new OpenAILLM(apiKey, baseUrl, model);
    }

    private LLMProvider createAnthropicLLM(LLMProviderConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Anthropic API key is required");
        String baseUrl = config.get("base-url", "https://api.anthropic.com");
        String model = config.get("model", "claude-3-5-sonnet-20241022");
        return new AnthropicLLM(apiKey, baseUrl, model);
    }

    private LLMProvider createAzureOpenAILLM(LLMProviderConfig config) {
        String apiKey = (String) config.get("api-key");
        String endpoint = (String) config.get("endpoint");
        String deploymentName = (String) config.get("deployment-name");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Azure OpenAI API key is required");
        if (endpoint == null || endpoint.isEmpty()) throw new ConfigurationException("Azure OpenAI endpoint is required");
        if (deploymentName == null || deploymentName.isEmpty()) throw new ConfigurationException("Azure OpenAI deployment name is required");
        String apiVersion = config.get("api-version", "2024-02-01");
        return new AzureOpenAILLM(apiKey, endpoint, deploymentName, apiVersion);
    }

    private LLMProvider createGeminiLLM(LLMProviderConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Gemini API key is required");
        String baseUrl = config.get("base-url", "https://generativelanguage.googleapis.com");
        String model = config.get("model", "gemini-1.5-flash");
        return new GeminiLLM(apiKey, baseUrl, model);
    }

    private LLMProvider createOllamaLLM(LLMProviderConfig config) {
        String baseUrl = config.get("base-url", "http://localhost:11434");
        String model = config.get("model", "llama3");
        return new OllamaLLM(baseUrl, model);
    }

    private LLMProvider createDeepSeekLLM(LLMProviderConfig config) {
        return new DeepSeekLLM(config);
    }

    private LLMProvider createGroqLLM(LLMProviderConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Groq API key is required");
        String baseUrl = config.get("base-url", "https://api.groq.com/openai/v1");
        String model = config.get("model", "llama-3.1-70b-versatile");
        return new GroqLLM(apiKey, baseUrl, model);
    }

    private LLMProvider createTogetherLLM(LLMProviderConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Together API key is required");
        String baseUrl = config.get("base-url", "https://api.together.xyz/v1");
        String model = config.get("model", "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo");
        return new TogetherLLM(apiKey, baseUrl, model);
    }

    private LLMProvider createVLLMLLM(LLMProviderConfig config) {
        String baseUrl = config.get("base-url", "http://localhost:8000/v1");
        String model = (String) config.get("model");
        return new VLLMLLM(baseUrl, model);
    }

    public static String[] getSupportedProviders() {
        return new String[]{"openai", "anthropic", "azure", "gemini", "ollama", "deepseek", "groq", "together", "vllm"};
    }
}
