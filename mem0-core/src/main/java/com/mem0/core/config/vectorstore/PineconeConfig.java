package com.mem0.core.config.vectorstore;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for Pinecone vector store provider.
 * Ported from Python mem0/configs/vectorstores/pinecone.py.
 *
 * @author MoBai

 */
public class PineconeConfig {

    private String apiKey;
    private String environment;
    private Map<String, Object> serverlessConfig = new HashMap<>();
    private Map<String, Object> podConfig = new HashMap<>();
    private boolean hybridSearch = false;
    private String metric = "cosine";
    private int batchSize = 100;
    private String namespace;
    private String indexName = "mem0";
    private int embeddingModelDims = 1536;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public Map<String, Object> getServerlessConfig() { return serverlessConfig; }
    public void setServerlessConfig(Map<String, Object> serverlessConfig) { this.serverlessConfig = serverlessConfig; }
    public Map<String, Object> getPodConfig() { return podConfig; }
    public void setPodConfig(Map<String, Object> podConfig) { this.podConfig = podConfig; }
    public boolean isHybridSearch() { return hybridSearch; }
    public void setHybridSearch(boolean hybridSearch) { this.hybridSearch = hybridSearch; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
