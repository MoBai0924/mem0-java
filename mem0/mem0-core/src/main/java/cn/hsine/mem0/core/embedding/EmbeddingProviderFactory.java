package cn.hsine.mem0.core.embedding;

import cn.hsine.mem0.core.embedding.impl.*;
import cn.hsine.mem0.core.exception.ConfigurationException;
import cn.hsine.mem0.core.embedding.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating EmbeddingProvider instances.
 *
 * @author MoBai

 */
@Component
public class EmbeddingProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingProviderFactory.class);

    public EmbeddingProvider create(EmbeddingConfig config) {
        String provider = config.getProvider();
        log.info("Creating EmbeddingProvider for provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAIEmbedding(config);
            case "ollama" -> createOllamaEmbedding(config);
            case "huggingface" -> createHuggingFaceEmbedding(config);
            case "azure" -> createAzureOpenAIEmbedding(config);
            case "gemini" -> createGeminiEmbedding(config);
            case "together" -> createTogetherEmbedding(config);
            case "vertexai" -> createVertexAIEmbedding(config);
            default -> throw new ConfigurationException("Unknown embedding provider: " + provider);
        };
    }

    private EmbeddingProvider createOpenAIEmbedding(EmbeddingConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("OpenAI API key not configured");
        String baseUrl = config.get("base-url", "https://api.openai.com/v1");
        String model = config.get("model", "text-embedding-3-small");
        int dimension = config.get("dimension", 1536);
        return new OpenAIEmbedding(apiKey, baseUrl, model, dimension);
    }

    private EmbeddingProvider createOllamaEmbedding(EmbeddingConfig config) {
        String baseUrl = config.get("base-url", "http://localhost:11434");
        String model = config.get("model", "nomic-embed-text");
        int dimension = config.get("dimension", 768);
        return new OllamaEmbedding(baseUrl, model, dimension);
    }

    private EmbeddingProvider createHuggingFaceEmbedding(EmbeddingConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("HuggingFace API key not configured");
        String baseUrl = config.get("base-url", "https://api-inference.huggingface.co/pipeline/feature-extraction");
        String model = config.get("model", "BAAI/bge-small-en-v1.5");
        int dimension = config.get("dimension", 384);
        return new HuggingFaceEmbedding(apiKey, baseUrl, model, dimension);
    }

    private EmbeddingProvider createAzureOpenAIEmbedding(EmbeddingConfig config) {
        String apiKey = (String) config.get("api-key");
        String endpoint = (String) config.get("endpoint");
        String deploymentName = (String) config.get("deployment-name");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Azure OpenAI API key not configured");
        if (endpoint == null || endpoint.isEmpty()) throw new ConfigurationException("Azure OpenAI endpoint not configured");
        if (deploymentName == null || deploymentName.isEmpty()) throw new ConfigurationException("Azure OpenAI deployment name not configured");
        String apiVersion = config.get("api-version", "2024-02-01");
        int dimension = config.get("dimension", 1536);
        return new AzureOpenAIEmbedding(apiKey, endpoint, deploymentName, apiVersion, dimension);
    }

    private EmbeddingProvider createGeminiEmbedding(EmbeddingConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Gemini API key not configured");
        String baseUrl = config.get("base-url", "https://generativelanguage.googleapis.com");
        String model = config.get("model", "text-embedding-004");
        int dimension = config.get("dimension", 768);
        return new GeminiEmbedding(apiKey, baseUrl, model, dimension);
    }

    private EmbeddingProvider createTogetherEmbedding(EmbeddingConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) throw new ConfigurationException("Together API key not configured");
        String baseUrl = config.get("base-url", "https://api.together.xyz/v1");
        String model = config.get("model", "togethercomputer/m2-bert-80M-8k-retrieval");
        int dimension = config.get("dimension", 768);
        return new TogetherEmbedding(apiKey, baseUrl, model, dimension);
    }

    private EmbeddingProvider createVertexAIEmbedding(EmbeddingConfig config) {
        String accessToken = (String) config.get("access-token");
        String projectId = (String) config.get("project-id");
        if (accessToken == null || accessToken.isEmpty()) throw new ConfigurationException("VertexAI access token not configured");
        if (projectId == null || projectId.isEmpty()) throw new ConfigurationException("VertexAI project ID not configured");
        String location = config.get("location", "us-central1");
        String model = config.get("model", "text-embedding-004");
        int dimension = config.get("dimension", 768);
        return new VertexAIEmbedding(accessToken, projectId, location, model, dimension);
    }

    public static String[] getSupportedProviders() {
        return new String[]{"openai", "ollama", "huggingface", "azure", "gemini", "together", "vertexai"};
    }
}
