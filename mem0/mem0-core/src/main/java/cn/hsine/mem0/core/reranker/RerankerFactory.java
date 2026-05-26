package cn.hsine.mem0.core.reranker;

import cn.hsine.mem0.core.exception.ConfigurationException;
import cn.hsine.mem0.core.llm.LLMProvider;
import cn.hsine.mem0.core.llm.LLMProviderFactory;
import cn.hsine.mem0.core.llm.LLMProviderConfig;
import cn.hsine.mem0.core.reranker.impl.CohereReranker;
import cn.hsine.mem0.core.reranker.impl.HuggingFaceReranker;
import cn.hsine.mem0.core.reranker.impl.LLMReranker;
import cn.hsine.mem0.core.reranker.impl.SentenceTransformerReranker;
import cn.hsine.mem0.core.reranker.impl.ZeroEntropyReranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating Reranker instances.
 * Ported from Python mem0/utils/factory.py RerankerFactory.
 *
 * @author MoBai

 */
@Component
public class RerankerFactory {

    private static final Logger log = LoggerFactory.getLogger(RerankerFactory.class);

    private final LLMProviderFactory llmProviderFactory;

    public RerankerFactory(LLMProviderFactory llmProviderFactory) {
        this.llmProviderFactory = llmProviderFactory;
    }

    /**
     * Creates a Reranker from configuration.
     *
     * @param provider the provider name
     * @param config   provider-specific configuration
     * @return the Reranker instance
     */
    public Reranker create(String provider, Map<String, Object> config) {
        log.info("Creating Reranker for provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "cohere" -> createCohereReranker(config);
            case "llm_reranker" -> createLLMReranker(config);
            case "sentence_transformer" -> createSentenceTransformerReranker(config);
            case "huggingface" -> createHuggingFaceReranker(config);
            case "zero_entropy" -> createZeroEntropyReranker(config);
            default -> throw new ConfigurationException("Unknown reranker provider: " + provider);
        };
    }

    private Reranker createCohereReranker(Map<String, Object> config) {
        String apiKey = config != null ? (String) config.get("api_key") : null;
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ConfigurationException("Cohere API key is required for reranker");
        }
        String model = config != null && config.get("model") != null
            ? (String) config.get("model") : "rerank-english-v3.0";
        return new CohereReranker(apiKey, model);
    }

    private Reranker createLLMReranker(Map<String, Object> config) {
        String model = config != null && config.get("model") != null
            ? (String) config.get("model") : "gpt-4o-mini";
        String provider = config != null && config.get("provider") != null
            ? (String) config.get("provider") : "openai";

        LLMProviderConfig llmConfig = new LLMProviderConfig();
        llmConfig.setProvider(provider);
        llmConfig.setConfig(config != null ? config : Map.of());

        LLMProvider llm = llmProviderFactory.create(llmConfig);
        return new LLMReranker(llm);
    }

    private Reranker createSentenceTransformerReranker(Map<String, Object> config) {
        String modelPath = config != null ? (String) config.get("model_path") : null;
        if (modelPath == null || modelPath.isEmpty()) {
            throw new ConfigurationException(
                "model_path is required for sentence_transformer reranker (path to ONNX model directory)");
        }
        int batchSize = config != null && config.get("batch_size") != null
            ? ((Number) config.get("batch_size")).intValue() : 32;
        return new SentenceTransformerReranker(modelPath, batchSize);
    }

    private Reranker createHuggingFaceReranker(Map<String, Object> config) {
        String modelPath = config != null ? (String) config.get("model_path") : null;
        if (modelPath == null || modelPath.isEmpty()) {
            throw new ConfigurationException(
                "model_path is required for huggingface reranker (path to ONNX model directory)");
        }
        int batchSize = config != null && config.get("batch_size") != null
            ? ((Number) config.get("batch_size")).intValue() : 32;
        int maxLength = config != null && config.get("max_length") != null
            ? ((Number) config.get("max_length")).intValue() : 512;
        boolean normalize = config != null && config.get("normalize") != null
            ? (boolean) config.get("normalize") : true;
        return new HuggingFaceReranker(modelPath, batchSize, maxLength, normalize);
    }

    private Reranker createZeroEntropyReranker(Map<String, Object> config) {
        String apiKey = config != null ? (String) config.get("api_key") : null;
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("ZERO_ENTROPY_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ConfigurationException(
                "Zero Entropy API key is required. Set ZERO_ENTROPY_API_KEY env var or pass api_key in config.");
        }
        String model = config != null && config.get("model") != null
            ? (String) config.get("model") : "zerank-1";
        return new ZeroEntropyReranker(apiKey, model);
    }

    /**
     * Returns the list of supported reranker providers.
     */
    public static String[] getSupportedProviders() {
        return new String[]{"cohere", "llm_reranker", "sentence_transformer", "huggingface", "zero_entropy"};
    }
}
