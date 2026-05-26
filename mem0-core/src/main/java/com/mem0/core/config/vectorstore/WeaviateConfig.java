package com.mem0.core.config.vectorstore;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for Weaviate vector store provider.
 * Ported from Python mem0/configs/vectorstores/weaviate.py.
 *
 * @author MoBai

 */
public class WeaviateConfig {

    private String clusterUrl;
    private Map<String, Object> authClientSecret = new HashMap<>();
    private Map<String, Object> additionalHeaders = new HashMap<>();
    private String collectionName = "Mem0";
    private int embeddingModelDims = 1536;

    public String getClusterUrl() { return clusterUrl; }
    public void setClusterUrl(String clusterUrl) { this.clusterUrl = clusterUrl; }
    public Map<String, Object> getAuthClientSecret() { return authClientSecret; }
    public void setAuthClientSecret(Map<String, Object> authClientSecret) { this.authClientSecret = authClientSecret; }
    public Map<String, Object> getAdditionalHeaders() { return additionalHeaders; }
    public void setAdditionalHeaders(Map<String, Object> additionalHeaders) { this.additionalHeaders = additionalHeaders; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
