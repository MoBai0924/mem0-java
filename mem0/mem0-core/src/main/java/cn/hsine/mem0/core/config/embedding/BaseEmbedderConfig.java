package cn.hsine.mem0.core.config.embedding;

import java.util.Map;
import java.util.HashMap;

/**
 * Base configuration for all embedding providers.
 * Ported from Python mem0/configs/embeddings/base.py.
 *
 * @author MoBai

 */
public abstract class BaseEmbedderConfig {

    private String model;
    private String apiKey;
    private Integer embeddingDims;
    private String ollamaBaseUrl;
    private String openaiBaseUrl;
    private Map<String, Object> modelKwargs = new HashMap<>();
    private String huggingfaceBaseUrl;
    private AzureConfig azureKwargs;
    private String httpClientProxies;
    private String vertexCredentialsJson;
    private String memoryAddEmbeddingType;
    private String memoryUpdateEmbeddingType;
    private String memorySearchEmbeddingType;
    private Integer outputDimensionality;
    private String lmstudioBaseUrl;
    private String awsAccessKeyId;
    private String awsSecretAccessKey;
    private String awsRegion;

    public BaseEmbedderConfig() {
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Integer getEmbeddingDims() { return embeddingDims; }
    public void setEmbeddingDims(Integer embeddingDims) { this.embeddingDims = embeddingDims; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public String getOpenaiBaseUrl() { return openaiBaseUrl; }
    public void setOpenaiBaseUrl(String openaiBaseUrl) { this.openaiBaseUrl = openaiBaseUrl; }
    public Map<String, Object> getModelKwargs() { return modelKwargs; }
    public void setModelKwargs(Map<String, Object> modelKwargs) { this.modelKwargs = modelKwargs; }
    public String getHuggingfaceBaseUrl() { return huggingfaceBaseUrl; }
    public void setHuggingfaceBaseUrl(String huggingfaceBaseUrl) { this.huggingfaceBaseUrl = huggingfaceBaseUrl; }
    public AzureConfig getAzureKwargs() { return azureKwargs; }
    public void setAzureKwargs(AzureConfig azureKwargs) { this.azureKwargs = azureKwargs; }
    public String getHttpClientProxies() { return httpClientProxies; }
    public void setHttpClientProxies(String httpClientProxies) { this.httpClientProxies = httpClientProxies; }
    public String getVertexCredentialsJson() { return vertexCredentialsJson; }
    public void setVertexCredentialsJson(String vertexCredentialsJson) { this.vertexCredentialsJson = vertexCredentialsJson; }
    public String getMemoryAddEmbeddingType() { return memoryAddEmbeddingType; }
    public void setMemoryAddEmbeddingType(String memoryAddEmbeddingType) { this.memoryAddEmbeddingType = memoryAddEmbeddingType; }
    public String getMemoryUpdateEmbeddingType() { return memoryUpdateEmbeddingType; }
    public void setMemoryUpdateEmbeddingType(String memoryUpdateEmbeddingType) { this.memoryUpdateEmbeddingType = memoryUpdateEmbeddingType; }
    public String getMemorySearchEmbeddingType() { return memorySearchEmbeddingType; }
    public void setMemorySearchEmbeddingType(String memorySearchEmbeddingType) { this.memorySearchEmbeddingType = memorySearchEmbeddingType; }
    public Integer getOutputDimensionality() { return outputDimensionality; }
    public void setOutputDimensionality(Integer outputDimensionality) { this.outputDimensionality = outputDimensionality; }
    public String getLmstudioBaseUrl() { return lmstudioBaseUrl; }
    public void setLmstudioBaseUrl(String lmstudioBaseUrl) { this.lmstudioBaseUrl = lmstudioBaseUrl; }
    public String getAwsAccessKeyId() { return awsAccessKeyId; }
    public void setAwsAccessKeyId(String awsAccessKeyId) { this.awsAccessKeyId = awsAccessKeyId; }
    public String getAwsSecretAccessKey() { return awsSecretAccessKey; }
    public void setAwsSecretAccessKey(String awsSecretAccessKey) { this.awsSecretAccessKey = awsSecretAccessKey; }
    public String getAwsRegion() { return awsRegion; }
    public void setAwsRegion(String awsRegion) { this.awsRegion = awsRegion; }

    /**
     * Azure-specific configuration for embedding providers.
     */
    public static class AzureConfig {
        private String apiKey;
        private String azureDeployment;
        private String azureEndpoint;
        private String apiVersion;
        private Map<String, String> defaultHeaders = new HashMap<>();

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getAzureDeployment() { return azureDeployment; }
        public void setAzureDeployment(String azureDeployment) { this.azureDeployment = azureDeployment; }
        public String getAzureEndpoint() { return azureEndpoint; }
        public void setAzureEndpoint(String azureEndpoint) { this.azureEndpoint = azureEndpoint; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public Map<String, String> getDefaultHeaders() { return defaultHeaders; }
        public void setDefaultHeaders(Map<String, String> defaultHeaders) { this.defaultHeaders = defaultHeaders; }
    }
}
